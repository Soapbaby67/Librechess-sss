package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticLogEntry(
    val id: Long,
    val timestampLabel: String,
    val elapsedMs: Long,
    val level: String, // "INFO", "OK", "WARN", "ERROR"
    val tag: String,
    val message: String,
    val stackTrace: String? = null
)

object AppDiagnosticLogger {

    private const val PREFS_NAME = "librechess_diagnostics"
    private const val KEY_LAST_CRASH = "last_crash_trace"

    private val startTimeMs = System.currentTimeMillis()
    private var nextId = 1L

    private val _logs = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<DiagnosticLogEntry>> = _logs.asStateFlow()

    private val _lastCrashTrace = MutableStateFlow<String?>(null)
    val lastCrashTraceFlow: StateFlow<String?> = _lastCrashTrace.asStateFlow()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCrash = prefs.getString(KEY_LAST_CRASH, null)
        if (!savedCrash.isNullOrBlank()) {
            _lastCrashTrace.value = savedCrash
            warn("CrashRecovery", "Обнаружен сохраненный стектрейс предыдущего сбоя", savedCrash)
        }

        info(
            "System",
            "Устройство: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})"
        )

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val trace = formatThrowable(throwable)
            val fullReport = "Поток: ${thread.name}\nОшибка: ${throwable.javaClass.simpleName}: ${throwable.message}\n\n$trace"
            runCatching {
                prefs.edit().putString(KEY_LAST_CRASH, fullReport).commit()
            }
            Log.e("LibreChessDiag", "Uncaught exception on ${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun clearSavedCrash(context: Context) {
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LAST_CRASH)
                .apply()
        }
        _lastCrashTrace.value = null
    }

    fun clearAllLogs(context: Context) {
        clearSavedCrash(context)
        _logs.value = emptyList()
        info("Logger", "Журнал диагностики очищен пользователем")
    }

    fun info(tag: String, message: String) {
        append("INFO", tag, message, null)
        Log.i("LibreChess/$tag", message)
    }

    fun ok(tag: String, message: String) {
        append("OK", tag, message, null)
        Log.i("LibreChess/$tag", "[OK] $message")
    }

    fun warn(tag: String, message: String, stackTrace: String? = null) {
        append("WARN", tag, message, stackTrace)
        Log.w("LibreChess/$tag", message)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val trace = throwable?.let { formatThrowable(it) }
        append("ERROR", tag, message, trace)
        Log.e("LibreChess/$tag", message, throwable)
    }

    private fun append(level: String, tag: String, message: String, stackTrace: String?) {
        val now = System.currentTimeMillis()
        val elapsed = (now - startTimeMs).coerceAtLeast(0L)
        val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(now))
        val entry = DiagnosticLogEntry(
            id = nextId++,
            timestampLabel = timeFmt,
            elapsedMs = elapsed,
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTrace
        )
        _logs.update { current ->
            (listOf(entry) + current).take(120)
        }
    }

    fun formatThrowable(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString().take(4000)
    }

    fun exportAllLogsAsText(): String {
        val sb = StringBuilder()
        sb.appendLine("=== LIBRECHESS STUDIO DIAGNOSTIC LOG ===")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
        _lastCrashTrace.value?.let {
            sb.appendLine("\n--- SAVED CRASH REPORT ---")
            sb.appendLine(it)
            sb.appendLine("--------------------------\n")
        }
        for (item in _logs.value.reversed()) {
            sb.appendLine("[+${item.elapsedMs}ms | ${item.timestampLabel}] [${item.level}] ${item.tag}: ${item.message}")
            if (!item.stackTrace.isNullOrBlank()) {
                sb.appendLine(item.stackTrace)
            }
        }
        return sb.toString()
    }
}
