package ChatApplication;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO 
{
	public void saveMessage(String sender, String content)
	{
		String sql = "INSERT INTO messages (sender, content) VALUES (?, ?)";
		
		try(Connection conn = DBHandler.getConnection(); 
				PreparedStatement pstmt = conn.prepareStatement(sql))
		{
			pstmt.setString(1, sender);
			pstmt.setString(2, content);
			pstmt.executeUpdate();
		}
		catch(SQLException e)
		{
			System.err.println("Database Insert Error: " + e.getMessage());
		}
	}
	
	public List<String> getChatHistory()
	{
		List<String> history = new ArrayList<>();
		String sql = "SELECT sender, content FROM messages ORDER BY timestamp ASC";
		
		try(Connection conn = DBHandler.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql))
		{
			while(rs.next())
			{
				history.add(rs.getString("sender") + ": " + rs.getString("content"));
			}
		}
		catch(SQLException e)
		{
			System.err.println("Database Fetch Error: " + e.getMessage());		
		}
		return history;
	}
}