package com.example.engine

enum class DiffType {
    EQUAL, ADD, DELETE
}

data class DiffLine(
    val type: DiffType,
    val oldLineNumber: Int?,
    val newLineNumber: Int?,
    val text: String
)

object DiffGenerator {

    /**
     * Computes a line-by-line diff using Longest Common Subsequence (LCS).
     */
    fun computeDiff(oldText: String, newText: String): List<DiffLine> {
        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.split("\n")
        val newLines = if (newText.isEmpty()) emptyList() else newText.split("\n")

        val n = oldLines.size
        val m = newLines.size

        // DP table for LCS
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0 until n) {
            for (j in 0 until m) {
                if (oldLines[i] == newLines[j]) {
                    dp[i + 1][j + 1] = dp[i][j] + 1
                } else {
                    dp[i + 1][j + 1] = maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val result = mutableListOf<DiffLine>()
        var i = n
        var j = m

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
                result.add(
                    DiffLine(
                        type = DiffType.EQUAL,
                        oldLineNumber = i,
                        newLineNumber = j,
                        text = oldLines[i - 1]
                    )
                )
                i--
                j--
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                result.add(
                    DiffLine(
                        type = DiffType.ADD,
                        oldLineNumber = null,
                        newLineNumber = j,
                        text = newLines[j - 1]
                    )
                )
                j--
            } else if (i > 0) {
                result.add(
                    DiffLine(
                        type = DiffType.DELETE,
                        oldLineNumber = i,
                        newLineNumber = null,
                        text = oldLines[i - 1]
                    )
                )
                i--
            }
        }

        return result.reversed()
    }

    /**
     * Formats diff lines into a unified patch string (e.g. `+ const a = 1; \n - var a = 0;`)
     */
    fun formatPatch(diffLines: List<DiffLine>): String {
        return diffLines.joinToString("\n") { line ->
            when (line.type) {
                DiffType.ADD -> "+ ${line.text}"
                DiffType.DELETE -> "- ${line.text}"
                DiffType.EQUAL -> "  ${line.text}"
            }
        }
    }
}
