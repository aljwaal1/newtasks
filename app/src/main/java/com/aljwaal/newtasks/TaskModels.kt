package com.aljwaal.newtasks

import java.util.Locale
import java.util.UUID

data class TaskPriority(
    val id: String,
    val label: String,
    val rank: Int,
    val isDefault: Boolean = false
) {
    companion object {
        val NORMAL = TaskPriority("normal", "عادية", 0, true)
        val MEDIUM = TaskPriority("medium", "متوسطة", 1, true)
        val URGENT = TaskPriority("urgent", "عاجلة", 2, true)
        val defaults = listOf(NORMAL, MEDIUM, URGENT)

        fun fromStored(
            value: String?,
            storedLabel: String? = null,
            storedRank: Int? = null
        ): TaskPriority {
            val raw = value.orEmpty().trim()
            defaults.firstOrNull { it.id.equals(raw, ignoreCase = true) }?.let { return it }
            return when (raw.uppercase(Locale.US)) {
                "LOW", "NORMAL" -> NORMAL
                "HIGH", "MEDIUM" -> MEDIUM
                "URGENT" -> URGENT
                else -> TaskPriority(
                    id = raw.ifBlank { "normal" },
                    label = storedLabel?.trim().orEmpty().ifBlank { raw.ifBlank { NORMAL.label } },
                    rank = storedRank ?: 1,
                    isDefault = false
                )
            }
        }
    }
}

enum class TaskStatus(val label: String) {
    PENDING("قيد التنفيذ"),
    COMPLETED("مكتملة");

    companion object {
        fun from(value: String?): TaskStatus = entries.firstOrNull { it.name == value } ?: PENDING
    }
}

enum class RepeatRule(val label: String) {
    NONE("بدون تكرار"),
    DAILY("يوميًا"),
    WEEKLY("أسبوعيًا"),
    MONTHLY("شهريًا"),
    YEARLY("سنويًا");

    companion object {
        fun from(value: String?): RepeatRule = entries.firstOrNull { it.name == value } ?: NONE
    }
}

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val category: String = "عام",
    val dueAtMillis: Long,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.PENDING,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val reminderEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long = 0L,
    val lastNotifiedAtMillis: Long = 0L
)

data class TaskStats(
    val today: Int,
    val upcoming: Int,
    val overdue: Int,
    val completed: Int,
    val total: Int
)
