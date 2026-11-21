package com.familyrecipes.android.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Chat's API uses UTC instants; old cached chat values without an offset also represent UTC. */
object ChatTime {
    private val dateTime=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val time=DateTimeFormatter.ofPattern("HH:mm")
    fun instant(raw:String?):Instant? {
        val value=raw?.trim()?.replace(' ','T')?.takeIf{it.isNotEmpty()}?:return null
        return runCatching{Instant.parse(value)}.getOrNull()
            ?:runCatching{OffsetDateTime.parse(value).toInstant()}.getOrNull()
            ?:runCatching{LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)}.getOrNull()
    }
    fun label(raw:String?,includeDate:Boolean=true,zone:ZoneId=ZoneId.systemDefault()):String =
        instant(raw)?.atZone(zone)?.format(if(includeDate)dateTime else time).orEmpty()
    fun differentDay(previous:String?,current:String?,zone:ZoneId=ZoneId.systemDefault()):Boolean =
        instant(previous)?.atZone(zone)?.toLocalDate()!=instant(current)?.atZone(zone)?.toLocalDate()
    fun separate(previous:String?,current:String?,zone:ZoneId=ZoneId.systemDefault()):Boolean {
        val before=instant(previous)?:return true
        val now=instant(current)?:return false
        return differentDay(previous,current,zone) || Duration.between(before,now).toMinutes()>=5
    }
}
