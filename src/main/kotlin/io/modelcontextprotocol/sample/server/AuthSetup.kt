package io.modelcontextprotocol.sample.server

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

fun main() {
    println("Setting up Google Calendar OAuth2 authentication...")
    
    val credentialsPath = System.getenv("GOOGLE_CREDENTIALS_PATH") 
        ?: "/Users/eduardoruesta/Workplace/google-calendar-mcp-server/credentials.json"
    
    val jsonFactory = JacksonFactory.getDefaultInstance()
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val scopes = listOf(CalendarScopes.CALENDAR)
    
    try {
        val inputStream = FileInputStream(credentialsPath)
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(inputStream))
        
        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, scopes
        )
        .setDataStoreFactory(FileDataStoreFactory(File("/Users/eduardoruesta/Workplace/google-calendar-mcp-server/tokens")))
        .setAccessType("offline")
        .build()
        
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        val credential = AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
        
        println("Authentication successful!")
        println("Access token: ${credential.accessToken}")
        println("Tokens saved to 'tokens' directory")
        println("You can now use the MCP server with Claude Desktop")
        
    } catch (e: Exception) {
        println("Error during authentication: ${e.message}")
        e.printStackTrace()
    }
}