package com.example.deepmule.data

data class ImportSyncConflict(
    val system: String,
    val title: String,
    val incomingPath: String,
    val existingPath: String
)

data class ImportSyncReport(
    val sourcesProcessed: Int,
    val imported: Int,
    val ignored: Int,
    val conflicts: List<ImportSyncConflict>,
    val errors: List<String>
) {
    val hasIssues: Boolean
        get() = conflicts.isNotEmpty() || errors.isNotEmpty()

    companion object {
        val Empty = ImportSyncReport(
            sourcesProcessed = 0,
            imported = 0,
            ignored = 0,
            conflicts = emptyList(),
            errors = emptyList()
        )
    }
}

