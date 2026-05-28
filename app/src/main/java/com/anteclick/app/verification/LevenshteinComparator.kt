package com.anteclick.app.verification

/**
 * Detects typosquatting by computing Levenshtein edit distance between the installed
 * package name and official banking package names.
 *
 * Uses the Wagner-Fischer dynamic programming algorithm with O(n) space optimization.
 */
object LevenshteinComparator {

    private val officialPackageNames = listOf(
        "com.sbi.lotusintouch",
        "com.snapwork.hdfc",
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user",
        "com.csam.icici.bank.imobile"
    )

    /**
     * Computes the Levenshtein edit distance between two strings using the
     * Wagner-Fischer DP algorithm with O(n) space (two-row optimization).
     */
    fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length

        // Edge cases
        if (m == 0) return n
        if (n == 0) return m

        // Use two rows for O(n) space
        var previousRow = IntArray(n + 1) { it }
        var currentRow = IntArray(n + 1)

        for (i in 1..m) {
            currentRow[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                currentRow[j] = minOf(
                    currentRow[j - 1] + 1,      // insertion
                    previousRow[j] + 1,          // deletion
                    previousRow[j - 1] + cost    // substitution
                )
            }
            // Swap rows
            val temp = previousRow
            previousRow = currentRow
            currentRow = temp
        }

        return previousRow[n]
    }

    /**
     * Returns true when the package name has an edit distance of 1-3 from any official
     * banking package name. Exact matches (distance 0) return false.
     */
    fun isTyposquatting(packageName: String): Boolean {
        return officialPackageNames.any { official ->
            val distance = levenshtein(packageName, official)
            distance in 1..3
        }
    }

    /**
     * Returns the closest official package name and its edit distance, or null if
     * no official package names exist.
     */
    fun getClosestMatch(packageName: String): Pair<String, Int>? {
        if (officialPackageNames.isEmpty()) return null

        var closestName = officialPackageNames[0]
        var closestDistance = levenshtein(packageName, closestName)

        for (i in 1 until officialPackageNames.size) {
            val distance = levenshtein(packageName, officialPackageNames[i])
            if (distance < closestDistance) {
                closestDistance = distance
                closestName = officialPackageNames[i]
            }
        }

        return Pair(closestName, closestDistance)
    }
}
