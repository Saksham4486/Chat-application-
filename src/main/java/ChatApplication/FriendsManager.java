package ChatApplication;

import java.util.*;

/**
 * FriendsManager — loads/saves friends from LocalDB.
 * Persists across restarts. No data loss.
 */
public class FriendsManager {
    private static FriendsManager instance;
    private final UserSession session = UserSession.getInstance();

    // In-memory state (loaded from disk on init)
    private final Map<String, Boolean> friends = new LinkedHashMap<>(); // name → online
    private final Map<String, List<String>> groups = new LinkedHashMap<>(); // groupName → members
    private final List<String> pendingRequests = new ArrayList<>();

    private FriendsManager() {}

    public static FriendsManager getInstance() {
        if (instance == null) instance = new FriendsManager();
        return instance;
    }

    /** Call after login to load saved friends and groups from disk */
    public void loadFromDB() {
        friends.clear();
        groups.clear();

        String username = session.getUsername();
        if (username == null) return;

        // Load friends
        for (String f : LocalDB.getFriends(username)) {
            friends.put(f, false);
        }

        // Load all groups this user is in
        groups.putAll(LocalDB.getAllGroups());
    }

    // ── Friends ───────────────────────────────────────────────────────────────
    public void addFriend(String username) {
        if (!friends.containsKey(username)) {
            friends.put(username, false);
            LocalDB.addFriend(session.getUsername(), username);
        }
    }

    public void removeFriend(String username) {
        friends.remove(username);
        LocalDB.removeFriend(session.getUsername(), username);
    }

    public boolean isFriend(String username) { return friends.containsKey(username); }

    public void setOnline(String username, boolean online) {
        if (friends.containsKey(username)) friends.put(username, online);
    }

    public Map<String, Boolean> getFriends() { return Collections.unmodifiableMap(friends); }

    // ── Pending ───────────────────────────────────────────────────────────────
    public void addPendingRequest(String from) {
        if (!pendingRequests.contains(from)) pendingRequests.add(from);
    }

    public List<String> getPendingRequests() { return Collections.unmodifiableList(pendingRequests); }

    public void acceptRequest(String from) {
        pendingRequests.remove(from);
        addFriend(from);
    }

    public void declineRequest(String from) { pendingRequests.remove(from); }

    // Groups 
    public void createGroup(String name, String creator) {
        groups.computeIfAbsent(name, k -> new ArrayList<>());
        if (!groups.get(name).contains(creator)) groups.get(name).add(creator);
        LocalDB.createGroup(name, creator);
    }

    public void joinGroup(String name, String username) {
        groups.computeIfAbsent(name, k -> new ArrayList<>());
        if (!groups.get(name).contains(username)) {
            groups.get(name).add(username);
            LocalDB.addMemberToGroup(name, username);
        }
    }

    public boolean groupExists(String name) { return groups.containsKey(name); }
    public Map<String, List<String>> getGroups() { return Collections.unmodifiableMap(groups); }
    public List<String> getGroupMembers(String name) {
        return groups.getOrDefault(name, Collections.emptyList());
    }

    /** Get all online users (from server broadcast) */
    private final Set<String> onlineUsers = new LinkedHashSet<>();
    public void setOnlineUsers(String[] users) {
        onlineUsers.clear();
        for (String u : users) if (!u.isBlank()) onlineUsers.add(u);
        for (String f : friends.keySet()) friends.put(f, onlineUsers.contains(f));
    }
    public Set<String> getOnlineUsers() { return Collections.unmodifiableSet(onlineUsers); }
}
