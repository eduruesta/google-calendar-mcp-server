# Google Calendar MCP Server

A Model Context Protocol (MCP) server for Google Calendar integration, providing CRUD operations for calendar events.

## Features

- **List Events**: Retrieve calendar events with optional filters
- **Create Events**: Create new calendar events with title, time, location, and attendees
- **Update Events**: Modify existing calendar events
- **Delete Events**: Remove calendar events

## Prerequisites

- Java 17 or higher
- Google Cloud Platform account with Calendar API enabled
- Service account credentials for Google Calendar API

## Setup

### 1. Google Cloud Setup

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google Calendar API:
   - Navigate to "APIs & Services" > "Library"
   - Search for "Google Calendar API"
   - Click "Enable"

### 2. Service Account Setup

1. In Google Cloud Console, go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "Service Account"
3. Fill in the service account details and create
4. Click on the created service account
5. Go to the "Keys" tab
6. Click "Add Key" > "Create New Key" > "JSON"
7. Download the JSON file and save it securely

### 3. Calendar Permissions

To allow the service account to access your calendar:

1. Open Google Calendar
2. Go to your calendar settings
3. Under "Share with specific people", add the service account email (found in the JSON file)
4. Give it "Make changes to events" permission

### 4. Environment Setup

Set the environment variable pointing to your credentials file:

```bash
export GOOGLE_CREDENTIALS_PATH="/Users/eduardoruesta/Workplace/google-calendar-mcp-server/credentials.json"
```

### 5. Build and Run

```bash
# Build the project
./gradlew build

# Run the server
./gradlew run
```

## MCP Tools

### list_events

List calendar events from Google Calendar.

**Parameters:**
- `calendar_id` (optional): Calendar ID, defaults to "primary"
- `max_results` (optional): Maximum number of events to return, defaults to 10
- `time_min` (optional): Lower bound for event start time (ISO 8601 format)
- `time_max` (optional): Upper bound for event start time (ISO 8601 format)

**Example:**
```json
{
  "name": "list_events",
  "arguments": {
    "max_results": 5,
    "time_min": "2024-01-01T00:00:00Z"
  }
}
```

### create_event

Create a new calendar event.

**Required Parameters:**
- `summary`: Event title
- `start_time`: Start time (ISO 8601 format)
- `end_time`: End time (ISO 8601 format)

**Optional Parameters:**
- `description`: Event description
- `location`: Event location
- `attendees`: Array of email addresses
- `calendar_id`: Calendar ID (defaults to "primary")

**Example:**
```json
{
  "name": "create_event",
  "arguments": {
    "summary": "Team Meeting",
    "description": "Weekly team sync",
    "start_time": "2024-01-15T10:00:00Z",
    "end_time": "2024-01-15T11:00:00Z",
    "location": "Conference Room A",
    "attendees": ["team@company.com"]
  }
}
```

### update_event

Update an existing calendar event.

**Required Parameters:**
- `event_id`: ID of the event to update

**Optional Parameters:**
- `summary`: New event title
- `description`: New event description
- `start_time`: New start time (ISO 8601 format)
- `end_time`: New end time (ISO 8601 format)
- `location`: New event location
- `attendees`: New array of email addresses
- `calendar_id`: Calendar ID (defaults to "primary")

**Example:**
```json
{
  "name": "update_event",
  "arguments": {
    "event_id": "abc123def456",
    "summary": "Updated Team Meeting",
    "location": "Conference Room B"
  }
}
```

### delete_event

Delete a calendar event.

**Required Parameters:**
- `event_id`: ID of the event to delete

**Optional Parameters:**
- `calendar_id`: Calendar ID (defaults to "primary")

**Example:**
```json
{
  "name": "delete_event",
  "arguments": {
    "event_id": "abc123def456"
  }
}
```

## Time Format

All time parameters should be in ISO 8601 format. Examples:
- `2024-01-15T10:00:00Z` (UTC)
- `2024-01-15T10:00:00-05:00` (with timezone offset)

## Error Handling

The server provides detailed error messages for common issues:
- Missing required parameters
- Invalid time formats
- Authentication failures
- Calendar access permission issues

## Development

### Running Tests

```bash
./gradlew test
```

### Building Distribution

```bash
./gradlew shadowJar
```

The built JAR will be available in `build/libs/`.

## Security Notes

- Store your Google service account credentials securely
- Never commit credentials to version control
- Use environment variables for credential paths
- Ensure your service account has minimal required permissions

## Troubleshooting

### Common Issues

1. **Authentication Error**: Verify that `GOOGLE_CREDENTIALS_PATH` points to a valid service account JSON file
2. **Permission Denied**: Ensure the service account email is added to your calendar with appropriate permissions
3. **Event Not Found**: Check that the event ID is correct and the event exists in the specified calendar
4. **Time Format Error**: Ensure all datetime parameters are in ISO 8601 format

## License

This project is licensed under the MIT License.