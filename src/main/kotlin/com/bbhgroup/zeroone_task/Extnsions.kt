package com.bbhgroup.zeroone_task

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

fun Date?.format(format: String): String {
    if (this == null) return ""
    return SimpleDateFormat(format).format(this)
}

fun Date.beginOfDateTime(): Date {
    val lD = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    return Date.from(lD.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

fun Date.endOfDateTime(): Date {
    val lD = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    return Date.from(lD.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
}