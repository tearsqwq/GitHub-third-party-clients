package com.kun.github.utils.file

object FileSizeFormatter {

    fun format(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)} KB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024))} MB"
        }
    }
}
