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
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import com.chaosinmotion.caredemo.server.database.Database;
import com.chaosinmotion.caredemo.server.json.ReturnResult;
import com.chaosinmotion.caredemo.server.json.UserReturnResult;
import com.chaosinmotion.caredemo.server.util.EMailUtil;
import com.chaosinmotion.caredemo.server.util.UserRecord;
import com.chaosinmotion.caredemo.shared.Errors;

/**
 *	Handles all requests centered around logging in, including user preferences.
 *
 *	This handles the following commands:
 *
 *		users/login				-- Log in the user
 *		users/logout			-- Log out the user
 *		users/resetPassword		-- Reset password
 *		users/forgotPassword	-- Forgot password
 *		users/changePassword	-- Change from old to new password
 *		users/userinfo			-- Get the current logged in user's info
 *
 *	Signing up is handled by a separate interface
 */
public class Users
{
	private static final long EXPIRES = 60 * 60 * 1000;
	
	private String serverURL;

	/**
	 * @param server
	 */
	public void setServer(String server)
	{
		serverURL = server;
	}

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
	public ReturnResult login(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
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

			int userID;
			UserReturnResult result;
			if (rs.next()) {
				userID = rs.getInt(1);
				String email = rs.getString(2);
				String name = rs.getString(3);
				
				result = new UserReturnResult(userID, email, name);
				
			} else {
				return new ReturnResult(Errors.INCORRECTCREDENTIALS,"Wrong credentials");
			}
			
			rs.close();
			ps.close();
			
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
	
	/**
	 * Handle user info.
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult userinfo(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
				
		try {
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT email, name "
							+ "FROM Users "
							+ "WHERE userid = ?");
			ps.setInt(1, u.getUserID());
			rs = ps.executeQuery();

			UserReturnResult result;
			if (rs.next()) {
				String email = rs.getString(1);
				String name = rs.getString(2);
				
				result = new UserReturnResult(u.getUserID(), email, name);
				
			} else {
				return new ReturnResult(Errors.NOTLOGGEDIN,"User no longer exists");
			}
			
			rs.close();
			ps.close();
			
			/*
			 * Copy the ACL from the stored ACL.
			 */
			
			result.setACL(u.getACL());
			return result;			
		}
		finally {
			if (c != null) c.close();
			if (ps != null) ps.close();
			if (rs != null) rs.close();
		}
	}
	
	/**
	 * Log out of the application. This simply erases the user record from the
	 * session object.
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 */
	public ReturnResult logout(String cmd, JSONObject request, HttpSession session)
	{
		session.removeAttribute("userid");
		return new ReturnResult();
	}

	
	/**
	 * Request to reset the password. This requires a token to be passed in
	 * that was e-mailed to the user.
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult resetPassword(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		String token = request.optString("token");
		String password = request.optString("password");
		
		if ((token == null) || (password == null)) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing Param");
		}

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			
			/*
			 * Remove old forgot records
			 */
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			ps = c.prepareStatement(
					"DELETE FROM ForgotPassword WHERE expires < ?");
			ps.setTimestamp(1,ts);
			ps.execute();
			ps.close();

			/*
			 * Find our forgot record
			 */
			ps = c.prepareStatement("SELECT userid FROM ForgotPassword WHERE token = ?");
			ps.setString(1,token);
			rs = ps.executeQuery();
			int userid = 0;
			while (rs.next()) {
				userid = rs.getInt(1);
			}
			rs.close();
			ps.close();
			
			if (userid == 0) {
				return new ReturnResult(Errors.INCORRECTCREDENTIALS,"Wrong token");
			}
			
			/*
			 * Update password
			 */
			
			ps = c.prepareStatement("UPDATE Users SET password = ? WHERE userid = ?");
			ps.setString(1,password);
			ps.setInt(2,userid);
			ps.execute();
			ps.close();
			
			/*
			 * Extract user information and return
			 */
			ps = c.prepareStatement(
					"SELECT userid, email, name " +
					"FROM Users " +
					"WHERE userid = ?");
			ps.setInt(1, userid);
			rs = ps.executeQuery();

			int userID;
			UserReturnResult result;
			if (rs.next()) {
				userID = rs.getInt(1);
				String email = rs.getString(2);
				String name = rs.getString(3);
				
				result = new UserReturnResult(userID, email, name);
				
			} else {
				return new ReturnResult(Errors.INCORRECTCREDENTIALS,"Wrong credentials");
			}
			
			rs.close();
			ps.close();
			
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
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
	
	/**
	 * Forgot password. This handles a situation where the user has forgotten
	 * the password
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult forgotPassword(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		String email = request.optString("email");
		
		if (email == null) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing Param");
		}

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			/*
			 * Remove old forgot records
			 */
			
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			ps = c.prepareStatement(
					"DELETE FROM ForgotPassword WHERE expires < ?");
			ps.setTimestamp(1,ts);
			ps.execute();
			ps.close();
			
			/*
			 * Find user
			 */
			ps = c.prepareStatement(
					"SELECT userid " +
					"FROM Users " +
					"WHERE email = ?");
			ps.setString(1, email);
			rs = ps.executeQuery();

			int userID = 0;
			while (rs.next()) {
				userID = rs.getInt(1);
			}
			rs.close();
			ps.close();
			
			if (userID == 0) return new ReturnResult();		/* Not found, ignore */
			
			/*
			 * Generate internal token
			 */
			String token = UUID.randomUUID().toString();
			
			
			/*
			 * Now insert a new record
			 */
			
			ps = c.prepareStatement("INSERT INTO ForgotPassword " +
								 	"    ( userid, token, expires ) " +
									"VALUES " +
									"    ( ?, ?, ?)");
			ps.setInt(1, userID);
			ps.setString(2, token);
			ts = new Timestamp(EXPIRES + System.currentTimeMillis());
			ps.setTimestamp(3, ts);
			ps.execute();
			ps.close();
			
			/*
			 * Send validation e-mail
			 */
			
			String forgotPasswordUrl = serverURL + "/forgot.html";
			EMailUtil.sendResetPassword(email, forgotPasswordUrl, token);
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}

		return new ReturnResult();
	}
	
	/**
	 * Change the password for the logged in user. This replaces the old
	 * password with the new password
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult changePassword(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		String oldPassword = request.optString("oldpassword");
		String newPassword = request.optString("newpassword");
		
		if ((oldPassword == null) || (newPassword == null)) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing Param");
		}

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT password " +
					"FROM Users " +
					"WHERE userid = ?");
			ps.setInt(1, u.getUserID());
			rs = ps.executeQuery();

			String foundPassword = null;
			while (rs.next()) {
				foundPassword = rs.getString(1);
			}
			rs.close();
			ps.close();
			
			/*
			 * Now determine if the password is a match. We compute the
			 * password by calculating (password) + SALT1 + token, to see
			 * if it matches what was sent.
			 */
		
			if (oldPassword.equalsIgnoreCase(foundPassword)) {
				/*
				 * Update password
				 */
				
				ps = c.prepareStatement("UPDATE Users SET password = ? WHERE userid = ?");
				ps.setString(1,newPassword);
				ps.setInt(2,u.getUserID());
				ps.execute();
				ps.close();
				return new ReturnResult();
			} else {
				return new ReturnResult(Errors.WRONGPASSWORD,"Wrong password");
			}
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
}
