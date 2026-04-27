//This module handles JDBC Connection 
package ChatApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBHandler 
{
	private static final String URL="jdbc:mysql://localhost:3306/chat_db";
	private static final String User="root";
	private static final String Pass="1234";
	
	public static Connection getConnection() throws SQLException
	{
		try 
		{
			Class.forName("com.mysql.cj.jdbc.Driver");
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("JDBC Driver not found!!");
		}
		
		return DriverManager.getConnection(URL,User,Pass);
	}
}
