/*	Onboard.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.commands;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import com.chaosinmotion.caredemo.server.database.Database;
import com.chaosinmotion.caredemo.server.json.ReturnResult;
import com.chaosinmotion.caredemo.server.json.UserReturnResult;
import com.chaosinmotion.caredemo.server.util.UserRecord;
import com.chaosinmotion.caredemo.shared.Errors;

/**
 * @author woody
 *
 */
public class Onboard
{
	public ReturnResult onboard(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		session.removeAttribute("userid");
		
		String username = request.optString("username");
		String password = request.optString("password");
		String token = request.optString("token");
		
		try {
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT userid, email, name "
							+ "FROM Users "
							+ "WHERE onboardtoken = ?");
			ps.setString(1, token);
			rs = ps.executeQuery();

			int userID;
			UserReturnResult result;
			if (rs.next()) {
				userID = rs.getInt(1);
				String email = rs.getString(2);
				String name = rs.getString(3);
				
				result = new UserReturnResult(userID, username, email, name);
				
			} else {
				return new ReturnResult(Errors.INCORRECTCREDENTIALS,"Wrong credentials");
			}
			
			rs.close();
			rs = null;
			ps.close();
			ps = null;
			
			/*
			 * Update username/password
			 */
			
			ps = c.prepareStatement("UPDATE Users " +
									"SET username = ?, " +
									"    password = ?, " +
									"    onboardtoken = NULL " +
									"WHERE userid = ?");
			ps.setString(1, username);
			ps.setString(2, password);
			ps.setInt(3, userID);
			ps.execute();			// will throw exception if username duplicate
			
			/*
			 * Get the ACL for this user.
			 */
			
			ps = c.prepareStatement("SELECT ace FROM UserAccessControlList WHERE userid = ?");
			ps.setInt(1, userID);
			rs = ps.executeQuery();
			HashSet<Integer> acl = new HashSet<Integer>();
			while (rs.next()) {
				acl.add(rs.getInt(1));
			}
			
			UserRecord ur = new UserRecord(userID,acl);
			session.setAttribute("userid", ur);
			result.setACL(acl);

			return result;			
		}
		finally {
			if (c != null) c.close();
			if (ps != null) ps.close();
			if (rs != null) rs.close();
		}
	}
}
