PeekEvent

Connecting communities through hyperlocal event discovery and promotion

PeekEvent is a mobile application designed to bridge the gap between local event organizers and community members in Kenya. Built with a mobile-first approach, it democratizes event promotion by providing an accessible platform for discovering, creating, and coordinating local events.

Features
For Event Attendees

Hyperlocal Discovery – Find events happening in your area

Smart Filtering – Filter by category, location, and date

RSVP System – Express interest and track your attendance

Tukutane Zone – Connect with other attendees for ride-sharing

For Event Organizers

Easy Event Creation – Simple form to create and publish events

Attendance Tracking – Real-time RSVP count and attendee management

Event Management – Edit or delete your created events

Media Support – Add images to make events more appealing

Core Functionality

User Authentication – Secure Firebase-based login system

Real-time Updates – Live synchronization across all users

Mobile-Optimized – Designed specifically for smartphones

Getting Started
Prerequisites

Android Studio Arctic Fox or later

Android SDK API Level 21 or higher

Java 8 or higher

Google Services JSON file (for Firebase)

Installation
git clone https://github.com/yourusername/peekevent.git
cd peekevent

Firebase Setup

Create a new Firebase project in the Firebase Console

Add an Android app to your project

Download google-services.json and place it in the app/ directory

Enable Authentication (Email/Password) and Realtime Database

Database Rules

{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}

Build and Run
./gradlew assembleDebug


Or open in Android Studio and click Run.

Architecture

PeekEvent follows a three-tier architecture:

Presentation Layer    - XML Layouts + Activities
Application Logic     - Java/Kotlin Business Logic
Data Layer            - Firebase Realtime Database


Key Components

Activities: MainActivity, CreateEventActivity, EventDetailActivity, ProfileActivity

Authentication: SignInActivity with Firebase Auth integration

Data Management: EventAdapter, RSVPManager

Firebase Services: Realtime Database, Authentication, Storage

Database Schema

Users Collection

{
  "users": {
    "userId": {
      "firstName": "string",
      "lastName": "string",
      "email": "string",
      "phoneNumber": "string",
      "dateCreated": "timestamp"
    }
  }
}


Events Collection

{
  "events": {
    "eventId": {
      "title": "string",
      "description": "string",
      "eventDate": "string",
      "eventTime": "string",
      "imageUrl": "string",
      "category": "string",
      "location": "string",
      "tukutaneZone": "string",
      "organizerId": "string",
      "rsvpCount": "number"
    }
  }
}


RSVP Collection

{
  "rsvps": {
    "rsvpId": {
      "userId": "string",
      "eventId": "string",
      "timestamp": "timestamp"
    }
  }
}

Technology Stack
Component	Technology
Frontend	Android (Java/Kotlin)
UI Framework	XML Layouts
Backend Database	Firebase Realtime Database
Authentication	Firebase Auth
Storage	Firebase Storage
Version Control	Git
IDE	Android Studio
Development Setup

Project Structure

app/
 ├── src/main/java/com/peekevent/
 │    ├── activities/       # Activity classes
 │    ├── adapters/         # RecyclerView adapters
 │    ├── models/           # Data models
 │    └── utils/            # Utility classes
 ├── src/main/res/
 │    ├── layout/           # XML layouts
 │    ├── drawable/         # Images and icons
 │    └── values/           # Strings, colors, styles
 └── google-services.json   # Firebase configuration


Key Dependencies

implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-database'
implementation 'com.google.firebase:firebase-storage'
implementation 'androidx.recyclerview:recyclerview'
implementation 'com.github.bumptech.glide:glide'

Contributing

We welcome contributions to PeekEvent!

Fork the repository

Create a feature branch (git checkout -b feature/amazing-feature)

Commit your changes (git commit -m 'Add amazing feature')

Push to the branch (git push origin feature/amazing-feature)

Open a Pull Request

License

This project is licensed under the MIT License – see the LICENSE file for details.

Screenshots
<p align="center"> <img src="https://github.com/user-attachments/assets/1102794d-d336-489b-bb04-af0f4875094f" width="200" /> <img src="https://github.com/user-attachments/assets/083c6bfb-8cfd-45be-9518-da9ddc47eb2a" width="200" /> <img src="https://github.com/user-attachments/assets/c8067a03-ddfa-4cb6-bcec-b9361c77c48a" width="200" /> </p> <p align="center"> <img src="https://github.com/user-attachments/assets/8d7fd33e-930d-4a81-8c11-1b9f4b7c3464" width="200" /> <img src="https://github.com/user-attachments/assets/6656662a-cadd-479e-adf2-dc8b7e736929" width="200" /> <img src="https://github.com/user-attachments/assets/0ca422c6-61f6-4c1d-84d0-bb55f7353a3a" width="200" /> </p> <p align="center"> <img src="https://github.com/user-attachments/assets/dc9a1fc6-9368-4419-8949-c704f7572c5c" width="200" /> <img src="https://github.com/user-attachments/assets/8880b52c-9d9f-46e4-91eb-50ff4ec47d2e" width="200" /> <img src="https://github.com/user-attachments/assets/e60a949c-6d19-4c98-9d00-dba9c406e3a4" width="200" /> </p>
