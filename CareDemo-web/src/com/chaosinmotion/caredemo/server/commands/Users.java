/*	Users.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.commands;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import com.chaosinmotion.caredemo.server.database.Database;
import com.chaosinmotion.caredemo.server.util.UserRecord;

/**
 *	Handles all requests centered around logging in, including user preferences.
 *
 *	This handles the following commands:
 *
 *		users/login				-- Log in the user
 *		users/logout			-- Log out the user
 *		users/resetpassword		-- Reset password
 *		users/forgotpassword	-- Forgot password
 *		users/changepassword	-- Change from old to new password
 *		users/updateprofile		-- Update the profile
 *
 *	Signing up is handled by a separate interface
 */
public class Users
{
	/**
	 * Handle login request.
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public JSONObject login(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		session.removeAttribute("userid");
		
		try {
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT userid, email, name "
							+ "FROM Users "
							+ "WHERE username = ? AND password = ?");
			ps.setString(1, request.optString("username"));
			ps.setString(2, request.optString("password"));
			rs = ps.executeQuery();
			JSONObject result = new JSONObject();

			if (rs.next()) {
				int userID = rs.getInt(1);
				
				result.put("success", true);
				result.put("userid", userID);
				result.put("email", rs.getString(2));
				result.put("name", rs.getString(3));
				
				UserRecord ur = new UserRecord(userID);
				session.setAttribute("userid", ur);
			} else {
				result.put("success", false);
			}
			
			return result;
		}
		finally {
			if (c != null) c.close();
			if (ps != null) ps.close();
			if (rs != null) rs.close();
		}
	}
}
