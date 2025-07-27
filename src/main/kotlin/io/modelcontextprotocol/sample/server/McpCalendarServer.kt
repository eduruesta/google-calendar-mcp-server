package io.modelcontextprotocol.sample.server

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import io.ktor.utils.io.streams.*
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.*

fun `run mcp server`() {
    val credentialsPath = System.getenv("GOOGLE_CREDENTIALS_PATH") 
        ?: "/Users/eduardoruesta/Workplace/google-calendar-mcp-server/credentials.json" // Default to credentials.json in project root
    
    val calendarService = GoogleCalendarService(credentialsPath)
    
    val server = Server(
        Implementation(
            name = "google-calendar",
            version = "1.0.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    )
    
    // List events tool
    server.addTool(
        name = "list_events",
        description = """
            List calendar events from Google Calendar.
            Parameters:
            - calendar_id (optional): Calendar ID, defaults to "primary"
            - max_results (optional): Maximum number of events to return, defaults to 10
            - time_min (optional): Lower bound for event start time (ISO 8601 format)
            - time_max (optional): Upper bound for event start time (ISO 8601 format)
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("calendar_id") {
                    put("type", "string")
                    put("description", "Calendar ID (defaults to 'primary')")
                }
                putJsonObject("max_results") {
                    put("type", "number")
                    put("description", "Maximum number of events to return")
                }
                putJsonObject("time_min") {
                    put("type", "string")
                    put("description", "Lower bound for event start time (ISO 8601 format)")
                }
                putJsonObject("time_max") {
                    put("type", "string")
                    put("description", "Upper bound for event start time (ISO 8601 format)")
                }
            },
            required = emptyList()
        )
    ) { request ->
        try {
            val calendarId = request.arguments["calendar_id"]?.jsonPrimitive?.content ?: "primary"
            val maxResults = request.arguments["max_results"]?.jsonPrimitive?.intOrNull ?: 10
            val timeMin = request.arguments["time_min"]?.jsonPrimitive?.content
            val timeMax = request.arguments["time_max"]?.jsonPrimitive?.content
            
            val events = calendarService.listEvents(calendarId, maxResults, timeMin, timeMax)
            
            val formattedEvents = events.map { event ->
                """
                Event ID: ${event.id}
                Title: ${event.summary}
                Description: ${event.description}
                Start: ${event.start}
                End: ${event.end}
                Location: ${event.location}
                Attendees: ${event.attendees.joinToString(", ")}
                """.trimIndent()
            }
            
            CallToolResult(content = formattedEvents.map { TextContent(it) })
        } catch (e: Exception) {
            CallToolResult(content = listOf(TextContent("Error listing events: ${e.message}")))
        }
    }
    
    // Create event tool
    server.addTool(
        name = "create_event",
        description = """
            Create a new calendar event in Google Calendar.
            Required parameters:
            - summary: Event title
            - start_time: Start time (ISO 8601 format)
            - end_time: End time (ISO 8601 format)
            Optional parameters:
            - description: Event description
            - location: Event location
            - attendees: Array of email addresses
            - calendar_id: Calendar ID (defaults to "primary")
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("summary") {
                    put("type", "string")
                    put("description", "Event title")
                }
                putJsonObject("start_time") {
                    put("type", "string")
                    put("description", "Start time (ISO 8601 format)")
                }
                putJsonObject("end_time") {
                    put("type", "string")
                    put("description", "End time (ISO 8601 format)")
                }
                putJsonObject("description") {
                    put("type", "string")
                    put("description", "Event description")
                }
                putJsonObject("location") {
                    put("type", "string")
                    put("description", "Event location")
                }
                putJsonObject("attendees") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                    put("description", "Array of attendee email addresses")
                }
                putJsonObject("calendar_id") {
                    put("type", "string")
                    put("description", "Calendar ID (defaults to 'primary')")
                }
            },
            required = listOf("summary", "start_time", "end_time")
        )
    ) { request ->
        try {
            val summary = request.arguments["summary"]?.jsonPrimitive?.content 
                ?: return@addTool CallToolResult(content = listOf(TextContent("Summary is required")))
            val startTime = request.arguments["start_time"]?.jsonPrimitive?.content 
                ?: return@addTool CallToolResult(content = listOf(TextContent("Start time is required")))
            val endTime = request.arguments["end_time"]?.jsonPrimitive?.content 
                ?: return@addTool CallToolResult(content = listOf(TextContent("End time is required")))
            
            val description = request.arguments["description"]?.jsonPrimitive?.content ?: ""
            val location = request.arguments["location"]?.jsonPrimitive?.content ?: ""
            val calendarId = request.arguments["calendar_id"]?.jsonPrimitive?.content ?: "primary"
            val attendees = request.arguments["attendees"]?.jsonArray?.map { 
                it.jsonPrimitive.content 
            } ?: emptyList()
            
            val event = calendarService.createEvent(
                summary, description, startTime, endTime, location, attendees, calendarId
            )
            
            val result = """
                Event created successfully!
                Event ID: ${event.id}
                Title: ${event.summary}
                Start: ${event.start}
                End: ${event.end}
                Location: ${event.location}
                Attendees: ${event.attendees.joinToString(", ")}
            """.trimIndent()
            
            CallToolResult(content = listOf(TextContent(result)))
        } catch (e: Exception) {
            CallToolResult(content = listOf(TextContent("Error creating event: ${e.message}")))
        }
    }
    
    // Update event tool
    server.addTool(
        name = "update_event",
        description = """
            Update an existing calendar event in Google Calendar.
            Required parameters:
            - event_id: ID of the event to update
            Optional parameters:
            - summary: New event title
            - description: New event description
            - start_time: New start time (ISO 8601 format)
            - end_time: New end time (ISO 8601 format)
            - location: New event location
            - attendees: New array of email addresses
            - calendar_id: Calendar ID (defaults to "primary")
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("event_id") {
                    put("type", "string")
                    put("description", "ID of the event to update")
                }
                putJsonObject("summary") {
                    put("type", "string")
                    put("description", "New event title")
                }
                putJsonObject("description") {
                    put("type", "string")
                    put("description", "New event description")
                }
                putJsonObject("start_time") {
                    put("type", "string")
                    put("description", "New start time (ISO 8601 format)")
                }
                putJsonObject("end_time") {
                    put("type", "string")
                    put("description", "New end time (ISO 8601 format)")
                }
                putJsonObject("location") {
                    put("type", "string")
                    put("description", "New event location")
                }
                putJsonObject("attendees") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                    put("description", "New array of attendee email addresses")
                }
                putJsonObject("calendar_id") {
                    put("type", "string")
                    put("description", "Calendar ID (defaults to 'primary')")
                }
            },
            required = listOf("event_id")
        )
    ) { request ->
        try {
            val eventId = request.arguments["event_id"]?.jsonPrimitive?.content 
                ?: return@addTool CallToolResult(content = listOf(TextContent("Event ID is required")))
            
            val summary = request.arguments["summary"]?.jsonPrimitive?.content
            val description = request.arguments["description"]?.jsonPrimitive?.content
            val startTime = request.arguments["start_time"]?.jsonPrimitive?.content
            val endTime = request.arguments["end_time"]?.jsonPrimitive?.content
            val location = request.arguments["location"]?.jsonPrimitive?.content
            val calendarId = request.arguments["calendar_id"]?.jsonPrimitive?.content ?: "primary"
            val attendees = request.arguments["attendees"]?.jsonArray?.map { 
                it.jsonPrimitive.content 
            }
            
            val event = calendarService.updateEvent(
                eventId, summary, description, startTime, endTime, location, attendees, calendarId
            )
            
            val result = """
                Event updated successfully!
                Event ID: ${event.id}
                Title: ${event.summary}
                Start: ${event.start}
                End: ${event.end}
                Location: ${event.location}
                Attendees: ${event.attendees.joinToString(", ")}
            """.trimIndent()
            
            CallToolResult(content = listOf(TextContent(result)))
        } catch (e: Exception) {
            CallToolResult(content = listOf(TextContent("Error updating event: ${e.message}")))
        }
    }
    
    // Delete event tool
    server.addTool(
        name = "delete_event",
        description = """
            Delete a calendar event from Google Calendar.
            Required parameters:
            - event_id: ID of the event to delete
            Optional parameters:
            - calendar_id: Calendar ID (defaults to "primary")
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("event_id") {
                    put("type", "string")
                    put("description", "ID of the event to delete")
                }
                putJsonObject("calendar_id") {
                    put("type", "string")
                    put("description", "Calendar ID (defaults to 'primary')")
                }
            },
            required = listOf("event_id")
        )
    ) { request ->
        try {
            val eventId = request.arguments["event_id"]?.jsonPrimitive?.content 
                ?: return@addTool CallToolResult(content = listOf(TextContent("Event ID is required")))
            val calendarId = request.arguments["calendar_id"]?.jsonPrimitive?.content ?: "primary"
            
            val success = calendarService.deleteEvent(eventId, calendarId)
            
            val result = if (success) {
                "Event deleted successfully!"
            } else {
                "Failed to delete event. Event may not exist or you may not have permission."
            }
            
            CallToolResult(content = listOf(TextContent(result)))
        } catch (e: Exception) {
            CallToolResult(content = listOf(TextContent("Error deleting event: ${e.message}")))
        }
    }
    
    val transport = StdioServerTransport(
        System.`in`.asInput(),
        System.out.asSink().buffered()
    )
    
    runBlocking {
        server.connect(transport)
        val done = Job()
        server.onClose {
            done.complete()
        }
        done.join()
    }
}