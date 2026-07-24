package com.aljwaal.newtasks

import android.content.Context
import java.io.File
import java.util.Calendar
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

object TaskRepository {
    private const val PREFS = "smart_tasks_data_v2"
    private const val KEY_DATA = "data"
    private val lock = Any()
    private val defaultCategories = listOf(
        "عام",
        "العمل",
        "المنزل",
        "الدراسة",
        "الصحة",
        "المواعيد"
    )

    fun list(context: Context): List<TaskItem> = synchronized(lock) {
        val root = readRoot(context)
        val priorities = prioritiesFromRoot(root)
        root.optJSONArray("tasks").toTaskList(priorities)
            .sortedWith(
                compareBy<TaskItem> { it.status == TaskStatus.COMPLETED }
                    .thenByDescending { it.priority.rank }
                    .thenBy { it.dueAtMillis }
            )
    }

    fun get(context: Context, id: String): TaskItem? =
        list(context).firstOrNull { it.id == id }

    fun save(context: Context, task: TaskItem) = synchronized(lock) {
        val normalizedPriority = task.priority.copy(
            label = NumberFormatUtils.latinDigits(task.priority.label).trim()
                .ifBlank { TaskPriority.NORMAL.label }
        )
        val normalized = task.copy(
            title = NumberFormatUtils.latinDigits(task.title).trim(),
            notes = NumberFormatUtils.latinDigits(task.notes),
            category = NumberFormatUtils.latinDigits(task.category).trim().ifBlank { "عام" },
            priority = normalizedPriority
        )
        val root = readRoot(context)
        val priorities = prioritiesFromRoot(root)
        val tasks = root.optJSONArray("tasks").toTaskList(priorities).toMutableList()
        val index = tasks.indexOfFirst { it.id == normalized.id }
        if (index >= 0) tasks[index] = normalized else tasks.add(normalized)
        root.put("tasks", JSONArray(tasks.map(::taskToJson)))
        ensureCustomPriorityStored(root, normalized.priority)
        writeRoot(context, root)
        AppLog.write(
            context,
            "TASK_SAVED",
            "id=${normalized.id} status=${normalized.status} priority=${normalized.priority.id} due=${normalized.dueAtMillis}"
        )
    }

    fun delete(context: Context, id: String) = synchronized(lock) {
        val root = readRoot(context)
        val priorities = prioritiesFromRoot(root)
        val tasks = root.optJSONArray("tasks").toTaskList(priorities).filterNot { it.id == id }
        root.put("tasks", JSONArray(tasks.map(::taskToJson)))
        writeRoot(context, root)
        AppLog.write(context, "TASK_DELETED", "id=$id")
    }

    fun markCompleted(
        context: Context,
        id: String,
        completed: Boolean = true
    ): TaskItem? {
        val current = get(context, id) ?: return null
        val updated = current.copy(
            status = if (completed) TaskStatus.COMPLETED else TaskStatus.PENDING,
            completedAtMillis = if (completed) System.currentTimeMillis() else 0L
        )
        save(context, updated)
        return updated
    }

    fun updateLastNotified(
        context: Context,
        id: String,
        value: Long = System.currentTimeMillis()
    ) {
        val current = get(context, id) ?: return
        save(context, current.copy(lastNotifiedAtMillis = value))
    }

    fun categories(context: Context): List<String> = synchronized(lock) {
        val root = readRoot(context)
        val priorities = prioritiesFromRoot(root)
        val stored = root.optJSONArray("categories").toStringList()
        (defaultCategories + stored + root.optJSONArray("tasks").toTaskList(priorities).map { it.category })
            .map { NumberFormatUtils.latinDigits(it).trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun addCategory(context: Context, value: String): Boolean = synchronized(lock) {
        val name = NumberFormatUtils.latinDigits(value).trim()
        if (name.isEmpty()) return false
        val root = readRoot(context)
        val categories = root.optJSONArray("categories").toStringList().toMutableList()
        if (categories.any { it.equals(name, ignoreCase = true) } ||
            defaultCategories.any { it == name }
        ) {
            return false
        }
        categories.add(name)
        root.put("categories", JSONArray(categories))
        writeRoot(context, root)
        true
    }

    fun deleteCategory(context: Context, value: String): Boolean = synchronized(lock) {
        if (defaultCategories.contains(value)) return false
        val root = readRoot(context)
        val categories = root.optJSONArray("categories").toStringList().filterNot { it == value }
        root.put("categories", JSONArray(categories))
        writeRoot(context, root)
        true
    }

    fun priorities(context: Context): List<TaskPriority> = synchronized(lock) {
        prioritiesFromRoot(readRoot(context))
    }

    fun addPriority(context: Context, value: String): Boolean = synchronized(lock) {
        val label = NumberFormatUtils.latinDigits(value).trim()
        if (label.isBlank()) return false
        val root = readRoot(context)
        val priorities = prioritiesFromRoot(root)
        if (priorities.any { it.label.equals(label, ignoreCase = true) }) return false
        val created = TaskPriority(
            id = "custom_${UUID.randomUUID()}",
            label = label,
            rank = TaskPriority.MEDIUM.rank,
            isDefault = false
        )
        val custom = root.optJSONArray("priorities").toPriorityList().toMutableList()
        custom.add(created)
        root.put("priorities", JSONArray(custom.map(::priorityToJson)))
        writeRoot(context, root)
        AppLog.write(context, "PRIORITY_ADDED", "id=${created.id} label=${created.label}")
        true
    }

    fun deletePriority(context: Context, priorityId: String): Boolean = synchronized(lock) {
        if (TaskPriority.defaults.any { it.id == priorityId }) return false
        val root = readRoot(context)
        val priorities = prioritiesFromRoot(root)
        val tasks = root.optJSONArray("tasks").toTaskList(priorities)
        if (tasks.any { it.priority.id == priorityId }) return false
        val custom = root.optJSONArray("priorities").toPriorityList()
            .filterNot { it.id == priorityId }
        root.put("priorities", JSONArray(custom.map(::priorityToJson)))
        writeRoot(context, root)
        AppLog.write(context, "PRIORITY_DELETED", "id=$priorityId")
        true
    }

    fun stats(context: Context, now: Long = System.currentTimeMillis()): TaskStats {
        val tasks = list(context)
        val todayEnd = NumberFormatUtils.endOfDay(now)
        val todayStart = NumberFormatUtils.startOfDay(now)
        return TaskStats(
            today = tasks.count {
                it.status == TaskStatus.PENDING && it.dueAtMillis in todayStart..todayEnd
            },
            upcoming = tasks.count {
                it.status == TaskStatus.PENDING && it.dueAtMillis > todayEnd
            },
            overdue = tasks.count {
                it.status == TaskStatus.PENDING && it.dueAtMillis < now
            },
            completed = tasks.count { it.status == TaskStatus.COMPLETED },
            total = tasks.size
        )
    }

    fun tasksOnDate(context: Context, millis: Long): List<TaskItem> =
        list(context).filter { NumberFormatUtils.sameDay(it.dueAtMillis, millis) }

    fun nextOccurrence(task: TaskItem, fromMillis: Long = task.dueAtMillis): Long? {
        if (task.repeatRule == RepeatRule.NONE) return null
        val calendar = Calendar.getInstance().apply { timeInMillis = fromMillis }
        do {
            when (task.repeatRule) {
                RepeatRule.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                RepeatRule.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatRule.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                RepeatRule.YEARLY -> calendar.add(Calendar.YEAR, 1)
                RepeatRule.NONE -> return null
            }
        } while (calendar.timeInMillis <= System.currentTimeMillis())
        return calendar.timeInMillis
    }

    fun exportJson(context: Context): String = synchronized(lock) {
        val root = readRoot(context)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("schemaVersion", 3)
        root.toString(2)
    }

    fun importJson(context: Context, json: String): Result<Int> = runCatching {
        val imported = JSONObject(json)
        val importedPriorities = imported.optJSONArray("priorities").toPriorityList()
        val available = (TaskPriority.defaults + importedPriorities).distinctBy { it.id }
        val tasks = imported.optJSONArray("tasks").toTaskList(available)
        require(tasks.all { it.title.isNotBlank() }) { "ملف النسخة الاحتياطية غير صالح" }
        synchronized(lock) {
            writeRoot(
                context,
                JSONObject().apply {
                    put("tasks", JSONArray(tasks.map(::taskToJson)))
                    put("categories", imported.optJSONArray("categories") ?: JSONArray())
                    put("priorities", JSONArray(importedPriorities.map(::priorityToJson)))
                    put("schemaVersion", 3)
                }
            )
        }
        AppLog.write(context, "BACKUP_IMPORTED", "tasks=${tasks.size}")
        tasks.size
    }

    fun createLocalBackup(context: Context): File {
        val directory = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(directory, "smart_tasks_backup.json")
        file.writeText(exportJson(context), Charsets.UTF_8)
        AppLog.write(context, "LOCAL_BACKUP_CREATED", "path=${file.absolutePath}")
        return file
    }

    fun restoreLocalBackup(context: Context): Result<Int> {
        val file = File(File(context.filesDir, "backups"), "smart_tasks_backup.json")
        return if (!file.exists()) {
            Result.failure(IllegalStateException("لا توجد نسخة احتياطية محلية"))
        } else {
            importJson(context, file.readText(Charsets.UTF_8))
        }
    }

    private fun readRoot(context: Context): JSONObject {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val text = prefs.getString(KEY_DATA, null)
        return runCatching {
            if (text.isNullOrBlank()) initialRoot() else JSONObject(text)
        }.getOrElse {
            AppLog.write(context, "DATA_READ_FAILED", it.message.orEmpty())
            initialRoot()
        }
    }

    private fun writeRoot(context: Context, root: JSONObject) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DATA, root.toString())
            .apply()
    }

    private fun initialRoot() = JSONObject().apply {
        put("schemaVersion", 3)
        put("tasks", JSONArray())
        put("categories", JSONArray())
        put("priorities", JSONArray())
    }

    private fun prioritiesFromRoot(root: JSONObject): List<TaskPriority> {
        val custom = root.optJSONArray("priorities").toPriorityList()
        val base = (TaskPriority.defaults + custom).distinctBy { it.id }.toMutableList()
        val taskPriorities = root.optJSONArray("tasks").toTaskList(base).map { it.priority }
        taskPriorities.forEach { taskPriority ->
            if (base.none { it.id == taskPriority.id }) base.add(taskPriority)
        }
        return base.sortedWith(compareBy<TaskPriority> { it.rank }.thenBy { it.label })
    }

    private fun ensureCustomPriorityStored(root: JSONObject, priority: TaskPriority) {
        if (priority.isDefault || TaskPriority.defaults.any { it.id == priority.id }) return
        val custom = root.optJSONArray("priorities").toPriorityList().toMutableList()
        if (custom.none { it.id == priority.id }) {
            custom.add(priority)
            root.put("priorities", JSONArray(custom.map(::priorityToJson)))
        }
    }

    private fun taskToJson(task: TaskItem) = JSONObject().apply {
        put("id", task.id)
        put("title", task.title)
        put("notes", task.notes)
        put("category", task.category)
        put("dueAtMillis", task.dueAtMillis)
        put("priority", task.priority.id)
        put("priorityLabel", task.priority.label)
        put("priorityRank", task.priority.rank)
        put("status", task.status.name)
        put("repeatRule", task.repeatRule.name)
        put("reminderEnabled", task.reminderEnabled)
        put("createdAtMillis", task.createdAtMillis)
        put("completedAtMillis", task.completedAtMillis)
        put("lastNotifiedAtMillis", task.lastNotifiedAtMillis)
    }

    private fun priorityToJson(priority: TaskPriority) = JSONObject().apply {
        put("id", priority.id)
        put("label", priority.label)
        put("rank", priority.rank)
    }

    private fun JSONArray?.toTaskList(availablePriorities: List<TaskPriority>): List<TaskItem> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val title = NumberFormatUtils.latinDigits(item.optString("title")).trim()
                if (title.isEmpty()) continue
                val storedPriority = item.optString("priority")
                val priority = availablePriorities.firstOrNull {
                    it.id.equals(storedPriority, ignoreCase = true)
                } ?: TaskPriority.fromStored(
                    storedPriority,
                    NumberFormatUtils.latinDigits(item.optString("priorityLabel")),
                    item.optInt("priorityRank", 1)
                )
                add(
                    TaskItem(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = title,
                        notes = NumberFormatUtils.latinDigits(item.optString("notes")),
                        category = NumberFormatUtils.latinDigits(
                            item.optString("category", "عام")
                        ),
                        dueAtMillis = item.optLong(
                            "dueAtMillis",
                            System.currentTimeMillis()
                        ),
                        priority = priority,
                        status = TaskStatus.from(item.optString("status")),
                        repeatRule = RepeatRule.from(item.optString("repeatRule")),
                        reminderEnabled = item.optBoolean("reminderEnabled", true),
                        createdAtMillis = item.optLong(
                            "createdAtMillis",
                            System.currentTimeMillis()
                        ),
                        completedAtMillis = item.optLong("completedAtMillis", 0L),
                        lastNotifiedAtMillis = item.optLong("lastNotifiedAtMillis", 0L)
                    )
                )
            }
        }
    }

    private fun JSONArray?.toPriorityList(): List<TaskPriority> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val label = NumberFormatUtils.latinDigits(item.optString("label")).trim()
                if (id.isBlank() || label.isBlank()) continue
                add(
                    TaskPriority(
                        id = id,
                        label = label,
                        rank = item.optInt("rank", TaskPriority.MEDIUM.rank),
                        isDefault = false
                    )
                )
            }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                NumberFormatUtils.latinDigits(optString(index)).trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let(::add)
            }
        }
    }
}
