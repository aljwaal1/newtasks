package com.aljwaal.newtasks

import android.content.Context
import java.io.File
import java.util.Calendar
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
        readRoot(context).optJSONArray("tasks").toTaskList()
            .sortedWith(
                compareBy<TaskItem> { it.status == TaskStatus.COMPLETED }
                    .thenByDescending { it.priority.rank }
                    .thenBy { it.dueAtMillis }
            )
    }

    fun get(context: Context, id: String): TaskItem? =
        list(context).firstOrNull { it.id == id }

    fun save(context: Context, task: TaskItem) = synchronized(lock) {
        val normalized = task.copy(
            title = NumberFormatUtils.latinDigits(task.title).trim(),
            notes = NumberFormatUtils.latinDigits(task.notes),
            category = NumberFormatUtils.latinDigits(task.category).trim().ifBlank { "عام" }
        )
        val root = readRoot(context)
        val tasks = root.optJSONArray("tasks").toTaskList().toMutableList()
        val index = tasks.indexOfFirst { it.id == normalized.id }
        if (index >= 0) tasks[index] = normalized else tasks.add(normalized)
        root.put("tasks", JSONArray(tasks.map(::taskToJson)))
        writeRoot(context, root)
        AppLog.write(
            context,
            "TASK_SAVED",
            "id=${normalized.id} status=${normalized.status} due=${normalized.dueAtMillis}"
        )
    }

    fun delete(context: Context, id: String) = synchronized(lock) {
        val root = readRoot(context)
        val tasks = root.optJSONArray("tasks").toTaskList().filterNot { it.id == id }
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
        val stored = root.optJSONArray("categories").toStringList()
        (defaultCategories + stored + root.optJSONArray("tasks").toTaskList().map { it.category })
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
        root.put("schemaVersion", 2)
        root.toString(2)
    }

    fun importJson(context: Context, json: String): Result<Int> = runCatching {
        val root = JSONObject(json)
        val tasks = root.optJSONArray("tasks").toTaskList()
        require(tasks.all { it.title.isNotBlank() }) { "ملف النسخة الاحتياطية غير صالح" }
        synchronized(lock) {
            writeRoot(
                context,
                JSONObject().apply {
                    put("tasks", JSONArray(tasks.map(::taskToJson)))
                    put("categories", root.optJSONArray("categories") ?: JSONArray())
                    put("schemaVersion", 2)
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
        put("schemaVersion", 2)
        put("tasks", JSONArray())
        put("categories", JSONArray())
    }

    private fun taskToJson(task: TaskItem) = JSONObject().apply {
        put("id", task.id)
        put("title", task.title)
        put("notes", task.notes)
        put("category", task.category)
        put("dueAtMillis", task.dueAtMillis)
        put("priority", task.priority.name)
        put("status", task.status.name)
        put("repeatRule", task.repeatRule.name)
        put("reminderEnabled", task.reminderEnabled)
        put("createdAtMillis", task.createdAtMillis)
        put("completedAtMillis", task.completedAtMillis)
        put("lastNotifiedAtMillis", task.lastNotifiedAtMillis)
    }

    private fun JSONArray?.toTaskList(): List<TaskItem> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val title = NumberFormatUtils.latinDigits(item.optString("title")).trim()
                if (title.isEmpty()) continue
                add(
                    TaskItem(
                        id = item.optString("id").ifBlank {
                            java.util.UUID.randomUUID().toString()
                        },
                        title = title,
                        notes = NumberFormatUtils.latinDigits(item.optString("notes")),
                        category = NumberFormatUtils.latinDigits(
                            item.optString("category", "عام")
                        ),
                        dueAtMillis = item.optLong(
                            "dueAtMillis",
                            System.currentTimeMillis()
                        ),
                        priority = TaskPriority.from(item.optString("priority")),
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
