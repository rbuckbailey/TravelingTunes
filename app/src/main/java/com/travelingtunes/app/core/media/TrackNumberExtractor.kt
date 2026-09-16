package com.travelingtunes.app.core.media

object TrackNumberExtractor {
    private val LEADING_TRACK_REGEX = Regex("""^(\d{1,4})[\s._\-]+(.+)$""")

    data class TrackAndTitleResult(
        val trackNumber: Int,
        val cleanTitle: String
    )

    /**
     * Resolves track number and cleaned song display title.
     * When [currentTrackNumber] <= 0 and [fileName] (or [title]) begins with a leading number,
     * extracts the leading number as the track number and excludes it from the song title display.
     */
    fun resolveTrackAndTitle(
        currentTrackNumber: Int,
        title: String,
        fileName: String
    ): TrackAndTitleResult {
        val cleanFileName = fileName.substringBeforeLast('.').ifBlank { fileName }.trim()
        val matchInFile = LEADING_TRACK_REGEX.find(cleanFileName)

        var resolvedTrackNumber = currentTrackNumber
        var resolvedTitle = title.trim()

        if (resolvedTrackNumber <= 0 && matchInFile != null) {
            val numStr = matchInFile.groupValues[1]
            val parsedNum = numStr.toIntOrNull()
            if (parsedNum != null && parsedNum in 1..9999) {
                resolvedTrackNumber = parsedNum
            }
        }

        if (matchInFile != null) {
            val fileExtractedTitle = matchInFile.groupValues[2].trim()
            val matchInTitle = LEADING_TRACK_REGEX.find(resolvedTitle)
            if (matchInTitle != null) {
                val titleNum = matchInTitle.groupValues[1].toIntOrNull()
                if (titleNum == null || titleNum == resolvedTrackNumber || currentTrackNumber <= 0) {
                    val candidateTitle = matchInTitle.groupValues[2].trim()
                    if (candidateTitle.isNotBlank()) {
                        resolvedTitle = candidateTitle
                    }
                }
            } else if (resolvedTitle.equals(cleanFileName, ignoreCase = true) && fileExtractedTitle.isNotBlank()) {
                resolvedTitle = fileExtractedTitle
            }
        } else if (resolvedTrackNumber > 0) {
            val matchInTitle = LEADING_TRACK_REGEX.find(resolvedTitle)
            if (matchInTitle != null) {
                val titleNum = matchInTitle.groupValues[1].toIntOrNull()
                if (titleNum == resolvedTrackNumber) {
                    val candidateTitle = matchInTitle.groupValues[2].trim()
                    if (candidateTitle.isNotBlank()) {
                        resolvedTitle = candidateTitle
                    }
                }
            }
        }

        return TrackAndTitleResult(
            trackNumber = resolvedTrackNumber,
            cleanTitle = resolvedTitle.ifBlank { title }
        )
    }
}
