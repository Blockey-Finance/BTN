package org.btn.common

import java.util.*

fun getBeginOfDate(date: Date):Date{
    val calendar = Calendar.getInstance();
    calendar.time = date;
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0)
    return calendar.time
}

fun getEndOfDate(date: Date):Date{
    val calendar = Calendar.getInstance();
    calendar.time = date;
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59)
    return calendar.time
}
