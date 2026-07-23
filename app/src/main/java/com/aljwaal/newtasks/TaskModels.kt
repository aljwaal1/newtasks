package com.aljwaal.newtasks

import java.util.UUID

enum class TaskPriority(val label: String, val rank: Int) {
    LOW("منخفضة", 0),
    NORMAL("عادية", 1),
    HIGH("عالية", 2),
    URGENT("عاجلة", 3);

    companion object {
        fun from(value: String?): TaskPriority = entries.firstOrNull { it.name == value } ?: NORMAL
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
