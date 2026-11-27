package com.heasafe.aura.security

import java.util.LinkedList

/** 会话级安全事件日志：线程安全、容量封顶；经 sink 脱敏后落盘到加密日志库。 */
class SecurityLog(private val capacity: Int = 300) {

    data class Event(val time: Long, val type: Type, val detail: String) {
        enum class Type { BLOCKED_URL, TRACKER, PERMISSION, INJECTION, CERTIFICATE, DOWNLOAD, OTHER }
    }

    private val lock = Any()
    private val events = LinkedList<Event>()

    /** 可选持久化 sink：由 ViewModel 接到 Logger，落盘跨会话日志。 */
    var sink: ((Event) -> Unit)? = null

    fun add(type: Event.Type, detail: String) = synchronized(lock) {
        val event = Event(System.currentTimeMillis(), type, redact(detail))
        events.addFirst(event)
        while (events.size > capacity) events.removeLast()
        sink?.invoke(event)
    }

    /** 脱敏：URL 只保留 scheme://host，剥离 path/query，避免凭据性 token 落盘。 */
    private fun redact(detail: String): String =
        URL_REGEX.replace(detail) { m -> m.groupValues[1] + "/…" }

    companion object {
        private val URL_REGEX = Regex("(https?://[^/\\s]+)[^\\s]*")
    }

    fun snapshot(): List<Event> = synchronized(lock) { events.toList() }

    fun clear() = synchronized(lock) { events.clear() }
}
