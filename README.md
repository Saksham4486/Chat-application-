# ChatApp — Simple Persistent Chat (Discord-style UI)

## ✅ No Google login. No internet. No database server needed.

---

## How Data is Stored (like WhatsApp)

All data lives in **~/.chatapp/** on your computer:

| File | Contains |
|---|---|
| `users.dat` | Usernames + SHA-256 password hashes |
| `friends_<user>.dat` | Your friends list (one per line) |
| `groups.dat` | Group names and members |
| `msgs_<channel>.dat` | Chat history for each channel/DM |
| `session.dat` | Auto-login: remembers you between restarts |

**Nothing is ever lost.** Close the app, reopen it — your friends, groups, and all messages are still there.

---

## How to Compile & Run

### Step 1 — Compile (no extra jars needed!)
```bash
# Windows
javac ServerMulti.java ClientHandler.java ChatApplication\*.java

# Mac / Linux  
javac ServerMulti.java ClientHandler.java ChatApplication/*.java
```

### Step 2 — Start Server (one terminal)
```bash
java ServerMulti
```

### Step 3 — Start Client (one or more terminals)
```bash
java ChatApplication.ChatLoginUI
```

---

## First Time Use

1. Click **Register** → enter username + password → done!
2. Next time you open the app → auto-logged in (no password needed)
3. Add friends by username → they appear online instantly when they join
4. Create a group → it's saved forever, anyone can join by name

---

## Features

| Feature | How it works |
|---|---|
| 👤 Login / Register | Simple username + password (SHA-256 hashed locally) |
| 🔄 Auto-login | Session remembered in `~/.chatapp/session.dat` |
| 👥 Friends | Saved to disk — survive app restarts |
| 💬 DMs | Private messages between two users |
| 🏰 Groups | Persistent rooms, anyone can join |
| 📜 Chat history | Last 100 messages loaded when you open a channel |
| 📎 File sharing | Images, videos, docs via chat channel |
| 💾 Backup | Export chat as JSON to Desktop |
| 😊 Emoji picker | Built-in emoji panel |

---

## Free Cloud — None Needed!

All your data is saved locally for free, forever.
The "☁ Backup" button exports to a JSON file you can keep anywhere (Google Drive, USB, email to yourself).
