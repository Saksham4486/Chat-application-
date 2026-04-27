
💬 J-Link Chat Application
A Robust Real-Time Communication System with Google Drive Integration

J-Link is a high-performance desktop chat application built using Java Swing and Socket Programming. Designed with a modern, Discord-inspired UI, it allows for real-time multi-client communication, group management, and secure chat history backups via the Google Drive API.

🚀 Key Features
Real-Time Communication: Multi-threaded server handling concurrent client connections using TCP/IP Sockets.

Modern UI/UX: Dark-themed interface featuring custom-drawn UI components, avatars, and responsive layouts.

Cloud Backup System: Export your conversations as JSON files and automatically sync them to your Google Drive.

Group & Private Messaging: Seamlessly switch between global channels, private DMs, and group chats.

Dynamic Connectivity: Support for LAN connections, allowing users on different laptops to chat over the same network.

Local Persistence: Integrated LocalDB to store user sessions and chat history for offline viewing.

🛠 Tech Stack
Language: Java (JDK 17+)

GUI: Java Swing & AWT (Advanced Window Toolkit)

Networking: Java Socket API

Cloud API: Google Drive API v3 (OAuth 2.0)

Build System: Maven

Data Format: JSON (for exports and configuration)

📂 Project Architecture

Shutterstock
Explore
Plaintext
FinalChatApp/
├── src/main/java/ChatApplication/
│   ├── DiscordMainUI.java      # Main Frontend & Event Handling
│   ├── ServerMulti.java        # Central Server Logic
│   ├── BackupManager.java      # JSON Serialization Logic
│   └── GoogleDriveManager.java # API Authentication & Uploads
├── src/main/resources/         # UI Assets & API Credentials
├── pom.xml                     # Project Dependencies (Maven)
└── .gitignore                  # Security & Build Rules


⚙️ Setup & Installation
1. Prerequisites
Java Development Kit (JDK) 17 or higher.

Apache Maven installed.

A credentials.json file from the Google Cloud Console with Drive API enabled.

2. Installation
Clone the repository:

Bash
git clone https://github.com/Saksham4486/Chat-application-.git
Navigate to the directory:

Bash
cd FinalChatApp
Install dependencies:

Bash
mvn clean install
3. Running the Application
Start the Server: Execute ServerMulti.java to begin listening for connections.

Start the Client: Execute DiscordMainUI.java.

Connection: Enter the server's IP address (use localhost if running on the same machine).

🔒 Security and Best Practices
This project follows professional security standards by:

Utilizing a .gitignore to prevent sensitive credentials.json and OAuth tokens from being exposed in version control.

Excluding the Maven target/ folder to keep the repository lightweight and source-focused.

👨‍💻 Author
Saksham Trivedi
B.Tech Computer Science & Engineering Student
Graphic Era University