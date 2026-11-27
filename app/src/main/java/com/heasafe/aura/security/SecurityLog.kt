package com.heasafe.aura.security

import java.util.LinkedList

/** 会话级安全事件日志：线程安全、容量封顶，绝不落盘。 */
class SecurityLog(private val capacity: Int = 300) {

    data class Event(val time: Long, val type: Type, val detail: String) {
        enum class Type { BLOCKED_URL, TRACKER, PERMISSION, INJECTION, CERTIFICATE, DOWNLOAD, OTHER }
    }

    private val lock = Any()
    private val events = LinkedList<Event>()

    fun add(type: Event.Type, detail: String) = synchronized(lock) {
        events.addFirst(Event(System.currentTimeMillis(), type, detail))
        while (events.size > capacity) events.removeLast()
    }

    fun snapshot(): List<Event> = synchronized(lock) { events.toList() }

    fun clear() = synchronized(lock) { events.clear() }
}
