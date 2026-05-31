package com.anteclick.app.warnings

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import androidx.core.net.toUri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.anteclick.app.ui.theme.AnteClickColors
import com.anteclick.app.ui.theme.AnteClickTheme
import com.anteclick.app.ui.theme.AnteClickType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * OverlayWarningManager
 *
 * Renders a phishing warning card directly over the foreground app using
 * WindowManager with TYPE_ACCESSIBILITY_OVERLAY.
 *
 * WHY this is more reliable than WarningActivity from a service:
 *
 *   Activity launch path problems:
 *     - Requires FLAG_ACTIVITY_NEW_TASK → MIUI blocks background Activity starts
 *     - Goes through Android task stack → Telegram stacking issues
 *     - Activity creation has ~200–400 ms overhead
 *     - singleTask + CLEAR_TASK can kill unrelated tasks on some OEMs
 *
 *   TYPE_ACCESSIBILITY_OVERLAY advantages:
 *     - Rendered directly by WindowManager in the same process as the service
 *     - No Activity lifecycle, no task stack, no intent routing
 *     - Appears instantly — WindowManager.addView() is synchronous on main thread
 *     - Works on MIUI/HyperOS without any special permissions
 *     - Requires only BIND_ACCESSIBILITY_SERVICE — no SYSTEM_ALERT_WINDOW needed
 *     - Sits above all app windows including Telegram, Chrome, Instagram
 *
 * Lifecycle:
 *   show()    → posts to main thread → WindowManager.addView() → animation plays
 *   dismiss() → posts to main thread → WindowManager.removeView()
 *   Auto-dismiss fires after AUTO_DISMISS_MS if user does not interact
 */
object OverlayWarningManager {

    private const val TAG             = "AnteClick"
    private const val AUTO_DISMISS_MS = 30_000L   // 30 s — enough time to read and decide
    private const val DEDUP_WINDOW_MS = 5_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    // Currently displayed overlay — null when nothing is showing
    @Volatile private var activeView: ComposeView? = null
    @Volatile private var activeLifecycle: OverlayLifecycleOwner? = null

    // Dedup: url → timestamp of last show — bounded LRU to prevent memory leak
    private val shownUrls = object : LinkedHashMap<String, Long>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>) = size > 100
    }

    private val autoDismissRunnable = Runnable { dismissOnMainThread() }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Shows the phishing warning overlay for [warning].
     *
     * Thread-safe — posts to main thread internally.
     * Dedup: same URL within DEDUP_WINDOW_MS is silently ignored.
     */
    fun show(
        context: Context,
        warning: ThreatWarning,
        popupToken: Long? = null,
        tokenProvider: (() -> Long)? = null,
        onAccepted: (() -> Unit)? = null
    ) {
        val now = System.currentTimeMillis()
        synchronized(shownUrls) {
            val last = shownUrls[warning.url] ?: 0L
            if (now - last < DEDUP_WINDOW_MS) {
                Log.d(TAG, "Overlay dedup — skipping: ${warning.url}")
                return
            }
            shownUrls[warning.url] = now
        }
        Log.d(TAG, "⚠ Overlay show queued for: ${warning.url}")
        mainHandler.post { showOnMainThread(context, warning, popupToken, tokenProvider, onAccepted) }
    }

    /** Dismisses the current overlay. Thread-safe. */
    fun dismiss() {
        mainHandler.post { dismissOnMainThread() }
    }

    /**
     * Exits the dangerous website by performing navigation actions.
     * Uses multiple strategies to ensure it works across all Android devices:
     * 1. Try GLOBAL_ACTION_BACK twice (first closes overlay context, second navigates back)
     * 2. Fallback: launch HOME screen
     * 3. Final fallback: launch Chrome with about:blank
     */
    fun exitDangerousWebsite(context: Context) {
        mainHandler.post {
            var success = false

            // Strategy 1: Use AccessibilityService GLOBAL_ACTION_BACK
            // We need to get the actual service instance
            val service = try {
                // The context should be the AccessibilityService itself
                if (context is android.accessibilityservice.AccessibilityService) {
                    context
                } else {
                    null
                }
            } catch (_: Exception) { null }

            if (service != null) {
                // Perform BACK action — this navigates back in the browser
                success = service.performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                )
                Log.d(TAG, "GLOBAL_ACTION_BACK result=$success")

                if (success) {
                    // On some devices, one BACK just dismisses the current page
                    // Schedule a second BACK after a short delay to ensure we leave the site
                    mainHandler.postDelayed({
                        service.performGlobalAction(
                            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                        )
                        Log.d(TAG, "Second GLOBAL_ACTION_BACK executed")
                    }, 300)
                    return@post
                }
            }

            // Strategy 2: Go to HOME screen (works on all devices)
            Log.d(TAG, "Fallback: launching HOME screen")
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(homeIntent)
                success = true
            } catch (e: Exception) {
                Log.e(TAG, "HOME intent failed: ${e.message}")
            }

            // Strategy 3: Open a safe blank page in the browser (last resort)
            if (!success) {
                Log.d(TAG, "Final fallback: opening about:blank")
                try {
                    val safeIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("about:blank")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(safeIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "about:blank fallback also failed: ${e.message}")
                }
            }
        }
    }

    // ── Main-thread rendering ─────────────────────────────────────────────────

    private fun showOnMainThread(
        context: Context,
        warning: ThreatWarning,
        popupToken: Long?,
        tokenProvider: (() -> Long)?,
        onAccepted: (() -> Unit)?
    ) {
        // Dismiss any existing overlay first
        dismissOnMainThread()

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            // TYPE_ACCESSIBILITY_OVERLAY: granted automatically with BIND_ACCESSIBILITY_SERVICE.
            // Sits above all app windows. No SYSTEM_ALERT_WINDOW required.
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // FLAG_NOT_TOUCH_MODAL: touches outside the overlay pass through to the app below.
            // FLAG_LAYOUT_IN_SCREEN: overlay spans the full screen width including status bar area.
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val lifecycleOwner = OverlayLifecycleOwner().also { it.start() }

        val view = ComposeView(context).apply {
            // Attach lifecycle owners so Compose runtime can function outside an Activity
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                AnteClickTheme {
                    OverlayCard(
                        warning    = warning,
                        onLeaveWebsite = {
                            mainHandler.removeCallbacks(autoDismissRunnable)
                            // Clear dedup so same URL can be detected again immediately
                            synchronized(shownUrls) { shownUrls.remove(warning.url) }
                            dismissOnMainThread()
                            exitDangerousWebsite(context)
                        },
                        onContinue = {
                            mainHandler.removeCallbacks(autoDismissRunnable)
                            dismissOnMainThread()
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, warning.url.toUri())
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Could not open URL after continue: ${warning.url}", e)
                            }
                        }
                    )
                }
            }
        }

        try {
            // Final stale-popup guard: render-time validation on main thread,
            // immediately before addView and after async queueing.
            if (popupToken != null && tokenProvider != null) {
                val activeToken = tokenProvider.invoke()
                if (popupToken != activeToken) {
                    Log.d(TAG, "Overlay rejected at render-time popupToken=$popupToken activeToken=$activeToken url=${warning.url}")
                    lifecycleOwner.stop()
                    return
                }
            }

            wm.addView(view, params)
            activeView      = view
            activeLifecycle = lifecycleOwner
            onAccepted?.invoke()
            Log.d(TAG, "Overlay added to WindowManager")
            mainHandler.postDelayed(autoDismissRunnable, AUTO_DISMISS_MS)
        } catch (e: Exception) {
            Log.e(TAG, "WindowManager.addView failed: ${e.message}", e)
            lifecycleOwner.stop()
            activeView      = null
            activeLifecycle = null
        }
    }

    private fun dismissOnMainThread() {
        val view      = activeView      ?: return
        val lifecycle = activeLifecycle ?: return
        activeView      = null
        activeLifecycle = null
        mainHandler.removeCallbacks(autoDismissRunnable)
        try {
            val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(view)
            Log.d(TAG, "Overlay removed from WindowManager")
        } catch (e: Exception) {
            Log.w(TAG, "removeView error: ${e.message}")
        }
        lifecycle.stop()
    }

    // ── Compose overlay card ──────────────────────────────────────────────────

    @androidx.compose.runtime.Composable
    private fun OverlayCard(
        warning: ThreatWarning,
        onLeaveWebsite: () -> Unit,
        onContinue: () -> Unit
    ) {
        // Slide down from top + fade in simultaneously
        val slideY = remember { Animatable(-80f) }
        val alpha  = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            coroutineScope {
                launch { slideY.animateTo(0f,  tween(durationMillis = 320)) }
                launch { alpha.animateTo(1f,   tween(durationMillis = 280)) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = slideY.value
                    this.alpha   = alpha.value
                }
        ) {
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = AnteClickColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ── Gradient header ───────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AnteClickColors.SbiNavy, AnteClickColors.PrimaryPurple)
                                ),
                                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier         = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(AnteClickColors.ErrorRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⚠", fontSize = 17.sp, color = Color.White)
                            }
                            Column {
                                Text(
                                    text       = "Phishing Risk Detected",
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                                Text(
                                    text  = "AnteClick detected a suspicious site",
                                    style = AnteClickType.caption,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                        }
                    }

                    // ── Body ──────────────────────────────────────────────────
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        // Risk severity label
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AnteClickColors.ErrorRed)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text  = "DANGEROUS PHISHING WEBSITE DETECTED",
                                style = AnteClickType.label,
                                color = Color.White
                            )
                        }

                        // Flagged URL box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AnteClickColors.ErrorRedSurface)
                                .border(
                                    1.dp,
                                    AnteClickColors.ErrorRed.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text     = warning.url.ifEmpty { "—" },
                                style    = AnteClickType.caption.copy(fontFamily = FontFamily.Monospace),
                                color    = AnteClickColors.ErrorRed,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Detection source badge
                        val (srcLabel, srcBg, srcFg) = when (warning.source) {
                            DetectionSource.LOCAL   ->
                                Triple("LOCAL ANALYSIS",    AnteClickColors.NavySurface,   AnteClickColors.SbiNavy)
                            DetectionSource.BACKEND ->
                                Triple("BACKEND VERIFIED",  AnteClickColors.PurpleSurface, AnteClickColors.PrimaryPurple)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(srcBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = srcLabel, style = AnteClickType.label, color = srcFg)
                        }

                        // Simplified user-friendly reasons — max 3 to keep overlay compact
                        if (warning.reasons.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                warning.reasons.take(3).map { simplifyReason(it) }.forEach { reason ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(AnteClickColors.ErrorRedSurface)
                                            .border(
                                                1.dp,
                                                AnteClickColors.ErrorRed.copy(alpha = 0.35f),
                                                RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text     = reason,
                                            style    = AnteClickType.caption,
                                            color    = AnteClickColors.ErrorRed,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Primary action — Leave Website
                        Button(
                            onClick  = onLeaveWebsite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AnteClickColors.SbiNavy
                            )
                        ) {
                            Text(
                                text  = "Leave Website",
                                style = AnteClickType.buttonText,
                                color = Color.White
                            )
                        }

                        // Secondary action — Continue Anyway
                        OutlinedButton(
                            onClick  = onContinue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape  = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp, AnteClickColors.SecondaryText.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text  = "Continue Anyway",
                                style = AnteClickType.buttonText,
                                color = AnteClickColors.SecondaryText
                            )
                        }

                        // Footer
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(text = "🛡", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text  = "Protected by AnteClick",
                                style = AnteClickType.caption,
                                color = AnteClickColors.SecondaryText
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── OverlayLifecycleOwner ─────────────────────────────────────────────────────
// ComposeView requires LifecycleOwner + ViewModelStoreOwner + SavedStateRegistryOwner
// attached to the view tree. Since we are outside an Activity/Fragment we provide
// a minimal implementation of all three that drives the Compose runtime correctly.

internal class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val vmStore           = ViewModelStore()
    private val savedStateCtrl    = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = vmStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateCtrl.savedStateRegistry

    fun start() {
        savedStateCtrl.performAttach()
        savedStateCtrl.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        vmStore.clear()
    }
}

// ── Reason simplification ─────────────────────────────────────────────────────
// Maps internal heuristic labels to user-friendly messages.
// Hides technical details like entropy, Levenshtein distance, and heuristic weights.

private fun simplifyReason(technicalReason: String): String = when {
    technicalReason.contains("banking keyword", ignoreCase = true) -> "Suspicious domain"
    technicalReason.contains("suspicious", ignoreCase = true) -> "Suspicious domain"
    technicalReason.contains("untrusted", ignoreCase = true) -> "Untrusted banking link"
    technicalReason.contains("typo", ignoreCase = true) -> "Possible impersonation"
    technicalReason.contains("phishing", ignoreCase = true) -> "Possible impersonation"
    technicalReason.contains("clone", ignoreCase = true) -> "Possible impersonation"
    technicalReason.contains("homograph", ignoreCase = true) -> "Suspicious characters"
    technicalReason.contains("punycode", ignoreCase = true) -> "Suspicious domain encoding"
    technicalReason.contains("levenshtein", ignoreCase = true) -> "Similar to known bank"
    technicalReason.contains("entropy", ignoreCase = true) -> "Randomized domain"
    technicalReason.contains("shortener", ignoreCase = true) -> "Hidden destination"
    technicalReason.contains("raw ip", ignoreCase = true) -> "Suspicious IP address"
    technicalReason.contains("apk", ignoreCase = true) -> "Malware download detected"
    technicalReason.contains("escalation", ignoreCase = true) -> "Untrusted banking link"
    technicalReason.contains("hyphen", ignoreCase = true) -> "Suspicious domain structure"
    technicalReason.contains("subdomain", ignoreCase = true) -> "Suspicious domain structure"
    technicalReason.contains("mixed script", ignoreCase = true) -> "Suspicious characters"
    else -> "Suspicious website"
}
