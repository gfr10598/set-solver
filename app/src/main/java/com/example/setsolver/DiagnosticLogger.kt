package com.example.setsolver

/**
 * Interface for diagnostic logging throughout the processing pipeline
 */
interface DiagnosticLogger {
    fun log(message: String)
    fun logSection(title: String)
    fun clear()
}

/**
 * No-op implementation for when diagnostics are disabled
 */
class NullDiagnosticLogger : DiagnosticLogger {
    override fun log(message: String) {}
    override fun logSection(title: String) {}
    override fun clear() {}
}
