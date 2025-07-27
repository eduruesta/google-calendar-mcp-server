package io.modelcontextprotocol.sample.server

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GoogleCalendarService(private val credentialsPath: String) {
    private val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
    private val httpTransport: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val scopes = listOf(CalendarScopes.CALENDAR)
    
    private fun getCredentials(): Credential {
        val tokensDir = File("/Users/eduardoruesta/Workplace/google-calendar-mcp-server/tokens")
        val inputStream = FileInputStream(credentialsPath)
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(inputStream))
        
        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, scopes
        )
        .setDataStoreFactory(FileDataStoreFactory(tokensDir))
        .setAccessType("offline")
        .build()
        
        // Check if we already have stored credentials
        val dataStore = flow.credentialDataStore
        val storedCredential = dataStore.get("user")
        
        if (storedCredential != null) {
            // Return existing credential without opening browser
            return flow.loadCredential("user")
        } else {
            // Only use browser flow if no credentials exist
            val receiver = LocalServerReceiver.Builder().setPort(8888).build()
            return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
        }
    }
    
    private fun getCalendarService(): Calendar {
        val credential = getCredentials()
        return Calendar.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("Google Calendar MCP Server")
            .build()
    }
    
    fun listEvents(
        calendarId: String = "primary",
        maxResults: Int = 10,
        timeMin: String? = null,
        timeMax: String? = null
    ): List<CalendarEventInfo> {
        val service = getCalendarService()
        
        val eventsRequest = service.events().list(calendarId)
            .setMaxResults(maxResults)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            
        timeMin?.let { eventsRequest.setTimeMin(DateTime(it)) }
        timeMax?.let { eventsRequest.setTimeMax(DateTime(it)) }
        
        val events = eventsRequest.execute()
        
        return events.items?.map { event ->
            CalendarEventInfo(
                id = event.id ?: "",
                summary = event.summary ?: "No title",
                description = event.description ?: "",
                start = formatDateTime(event.start),
                end = formatDateTime(event.end),
                location = event.location ?: "",
                attendees = event.attendees?.map { it.email } ?: emptyList()
            )
        } ?: emptyList()
    }
    
    fun createEvent(
        summary: String,
        description: String = "",
        startDateTime: String,
        endDateTime: String,
        location: String = "",
        attendeeEmails: List<String> = emptyList(),
        calendarId: String = "primary"
    ): CalendarEventInfo {
        val service = getCalendarService()
        
        val event = Event().apply {
            this.summary = summary
            this.description = description
            this.location = location
            
            start = EventDateTime().apply {
                dateTime = DateTime(startDateTime)
                timeZone = "UTC"
            }
            
            end = EventDateTime().apply {
                dateTime = DateTime(endDateTime)
                timeZone = "UTC"
            }
            
            if (attendeeEmails.isNotEmpty()) {
                attendees = attendeeEmails.map { email ->
                    com.google.api.services.calendar.model.EventAttendee().apply {
                        this.email = email
                    }
                }
            }
        }
        
        val createdEvent = service.events().insert(calendarId, event).execute()
        
        return CalendarEventInfo(
            id = createdEvent.id,
            summary = createdEvent.summary,
            description = createdEvent.description ?: "",
            start = formatDateTime(createdEvent.start),
            end = formatDateTime(createdEvent.end),
            location = createdEvent.location ?: "",
            attendees = createdEvent.attendees?.map { it.email } ?: emptyList()
        )
    }
    
    fun updateEvent(
        eventId: String,
        summary: String? = null,
        description: String? = null,
        startDateTime: String? = null,
        endDateTime: String? = null,
        location: String? = null,
        attendeeEmails: List<String>? = null,
        calendarId: String = "primary"
    ): CalendarEventInfo {
        val service = getCalendarService()
        
        val existingEvent = service.events().get(calendarId, eventId).execute()
        
        summary?.let { existingEvent.summary = it }
        description?.let { existingEvent.description = it }
        location?.let { existingEvent.location = it }
        
        startDateTime?.let {
            existingEvent.start = EventDateTime().apply {
                dateTime = DateTime(it)
                timeZone = "UTC"
            }
        }
        
        endDateTime?.let {
            existingEvent.end = EventDateTime().apply {
                dateTime = DateTime(it)
                timeZone = "UTC"
            }
        }
        
        attendeeEmails?.let { emails ->
            existingEvent.attendees = emails.map { email ->
                com.google.api.services.calendar.model.EventAttendee().apply {
                    this.email = email
                }
            }
        }
        
        val updatedEvent = service.events().update(calendarId, eventId, existingEvent).execute()
        
        return CalendarEventInfo(
            id = updatedEvent.id,
            summary = updatedEvent.summary,
            description = updatedEvent.description ?: "",
            start = formatDateTime(updatedEvent.start),
            end = formatDateTime(updatedEvent.end),
            location = updatedEvent.location ?: "",
            attendees = updatedEvent.attendees?.map { it.email } ?: emptyList()
        )
    }
    
    fun deleteEvent(eventId: String, calendarId: String = "primary"): Boolean {
        return try {
            val service = getCalendarService()
            service.events().delete(calendarId, eventId).execute()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun formatDateTime(eventDateTime: EventDateTime?): String {
        if (eventDateTime?.dateTime != null) {
            val instant = Instant.ofEpochMilli(eventDateTime.dateTime.value)
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
        return eventDateTime?.date?.toString() ?: ""
    }
}

@Serializable
data class CalendarEventInfo(
    val id: String,
    val summary: String,
    val description: String,
    val start: String,
    val end: String,
    val location: String,
    val attendees: List<String>
)