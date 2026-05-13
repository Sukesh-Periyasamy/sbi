package com.trustshield.app.warnings

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
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
import com.trustshield.app.ui.theme.TrustShieldColors
import com.trustshield.app.ui.theme.TrustShieldTheme
import com.trustshield.app.ui.theme.TrustShieldType
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

    private const val TAG             = "TrustShield"
    private const val AUTO_DISMISS_MS = 30_000L   // 30 s — enough time to read and decide
    private const val DEDUP_WINDOW_MS = 5_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    // Currently displayed overlay — null when nothing is showing
    @Volatile private var activeView: ComposeView? = null
    @Volatile private var activeLifecycle: OverlayLifecycleOwner? = null

    // Dedup: url → timestamp of last show
    private val shownUrls = mutableMapOf<String, Long>()

    private val autoDismissRunnable = Runnable { dismissOnMainThread() }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Shows the phishing warning overlay for [warning].
     *
     * Thread-safe — posts to main thread internally.
     * Dedup: same URL within DEDUP_WINDOW_MS is silently ignored.
     */
    fun show(context: Context, warning: ThreatWarning) {
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
        mainHandler.post { showOnMainThread(context, warning) }
    }

    /** Dismisses the current overlay. Thread-safe. */
    fun dismiss() {
        mainHandler.post { dismissOnMainThread() }
    }

    // ── Main-thread rendering ─────────────────────────────────────────────────

    private fun showOnMainThread(context: Context, warning: ThreatWarning) {
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
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        val lifecycleOwner = OverlayLifecycleOwner().also { it.start() }

        val view = ComposeView(context).apply {
            // Attach lifecycle owners so Compose runtime can function outside an Activity
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                TrustShieldTheme {
                    OverlayCard(
                        warning    = warning,
                        onDismiss  = {
                            mainHandler.removeCallbacks(autoDismissRunnable)
                            dismissOnMainThread()
                        },
                        onContinue = {
                            mainHandler.removeCallbacks(autoDismissRunnable)
                            dismissOnMainThread()
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(warning.url))
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
            wm.addView(view, params)
            activeView      = view
            activeLifecycle = lifecycleOwner
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
        onDismiss: () -> Unit,
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
                colors    = CardDefaults.cardColors(containerColor = TrustShieldColors.SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ── Gradient header ───────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(TrustShieldColors.SbiNavy, TrustShieldColors.PrimaryPurple)
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
                                    .background(TrustShieldColors.ErrorRed),
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
                                    text  = "TrustShield intercepted a suspicious site",
                                    style = TrustShieldType.caption,
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

                        // Risk badge + score row
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TrustShieldColors.ErrorRed)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text  = "HIGH RISK",
                                    style = TrustShieldType.label,
                                    color = Color.White
                                )
                            }
                            Text(
                                text       = "Score: ${warning.score}",
                                style      = TrustShieldType.caption,
                                color      = TrustShieldColors.ErrorRed,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Flagged URL box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TrustShieldColors.ErrorRedSurface)
                                .border(
                                    1.dp,
                                    TrustShieldColors.ErrorRed.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text     = warning.url.ifEmpty { "—" },
                                style    = TrustShieldType.caption.copy(fontFamily = FontFamily.Monospace),
                                color    = TrustShieldColors.ErrorRed,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Detection source badge
                        val (srcLabel, srcBg, srcFg) = when (warning.source) {
                            DetectionSource.LOCAL   ->
                                Triple("LOCAL ANALYSIS",    TrustShieldColors.NavySurface,   TrustShieldColors.SbiNavy)
                            DetectionSource.BACKEND ->
                                Triple("BACKEND VERIFIED",  TrustShieldColors.PurpleSurface, TrustShieldColors.PrimaryPurple)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(srcBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = srcLabel, style = TrustShieldType.label, color = srcFg)
                        }

                        // Signal chips — max 3 to keep overlay compact
                        if (warning.reasons.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                warning.reasons.take(3).forEach { reason ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(TrustShieldColors.ErrorRedSurface)
                                            .border(
                                                1.dp,
                                                TrustShieldColors.ErrorRed.copy(alpha = 0.35f),
                                                RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text     = reason,
                                            style    = TrustShieldType.caption,
                                            color    = TrustShieldColors.ErrorRed,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Primary action — Close Browser
                        Button(
                            onClick  = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrustShieldColors.SbiNavy
                            )
                        ) {
                            Text(
                                text  = "Close Browser",
                                style = TrustShieldType.buttonText,
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
                                1.dp, TrustShieldColors.SecondaryText.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text  = "Continue Anyway",
                                style = TrustShieldType.buttonText,
                                color = TrustShieldColors.SecondaryText
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
                                text  = "Protected by TrustShield",
                                style = TrustShieldType.caption,
                                color = TrustShieldColors.SecondaryText
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
