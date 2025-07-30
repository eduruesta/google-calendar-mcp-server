package io.modelcontextprotocol.sample.server

import java.text.SimpleDateFormat
import java.util.*

object DateTimeTools {
    private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val ISO_TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val ISO_DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    
    init {
        // Configure formatters to use UTC by default
        ISO_DATE_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
        ISO_TIME_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
        ISO_DATETIME_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
    }

    data class CurrentDateTimeResult(
        val datetime: String,
        val date: String,
        val time: String,
        val timezone: String
    )

    data class AddDateTimeResult(
        val date: String,
        val originalDate: String,
        val daysAdded: Int,
        val hoursAdded: Int,
        val minutesAdded: Int
    )

    fun getCurrentDateTime(timezone: String = "UTC"): CurrentDateTimeResult {
        val timeZone = try {
            TimeZone.getTimeZone(timezone)
        } catch (_: Exception) {
            TimeZone.getTimeZone("UTC")
        }

        val now = Date()
        val calendar = Calendar.getInstance(timeZone)
        calendar.time = now
        
        // Create formatters for this specific timezone
        val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            this.timeZone = timeZone
        }
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
            this.timeZone = timeZone
        }

        return CurrentDateTimeResult(
            datetime = dateTimeFormatter.format(now),
            date = dateFormatter.format(now),
            time = timeFormatter.format(now),
            timezone = timeZone.id
        )
    }

    fun addDateTime(date: String, days: Int, hours: Int, minutes: Int): AddDateTimeResult {
        val baseDate = if (date.isNotBlank()) {
            try {
                ISO_DATE_FORMAT.parse(date)
            } catch (_: Exception) {
                // Use current date if parsing fails
                Date()
            }
        } else {
            Date()
        }

        // Use Calendar to handle date arithmetic
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = baseDate
        
        // Add the specified amounts
        calendar.add(Calendar.DAY_OF_MONTH, days)
        calendar.add(Calendar.HOUR_OF_DAY, hours)
        calendar.add(Calendar.MINUTE, minutes)

        // Format the result date
        val resultDate = ISO_DATE_FORMAT.format(calendar.time)

        return AddDateTimeResult(
            date = resultDate,
            originalDate = date,
            daysAdded = days,
            hoursAdded = hours,
            minutesAdded = minutes
        )
    }
}