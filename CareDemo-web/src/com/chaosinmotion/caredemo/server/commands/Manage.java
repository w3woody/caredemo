/*	Search.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.commands;

import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import com.chaosinmotion.caredemo.server.database.Database;
import com.chaosinmotion.caredemo.server.json.ReturnResult;
import com.chaosinmotion.caredemo.server.json.SearchResult;
import com.chaosinmotion.caredemo.server.json.SimpleReturnResult;
import com.chaosinmotion.caredemo.server.json.UserReturnResult;
import com.chaosinmotion.caredemo.server.util.EMailUtil;
import com.chaosinmotion.caredemo.server.util.UserRecord;
import com.chaosinmotion.caredemo.shared.ACE;
import com.chaosinmotion.caredemo.shared.Errors;

/**
 * 	Handles requests centered around managing users. This contains the code
 * 	not part of the profile group which allows us to create, remove and modify
 * 	users as well as search for them. This includes:
 * 
 * 		manage/search				-- Search for users
 * 		manage/userInfo				-- Basic user info by e-mail address
 * 		manage/addUser				-- Add a new user
 * 		manage/removeUser			-- Remove a user
 * 
 * @author woody
 *
 */
public class Manage
{
	private String serverURL;

	/**
	 * @param server
	 */
	public void setServer(String server)
	{
		serverURL = server;
	}

	/**
	 * Search
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult search(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		
		int typePage = request.optInt("type",0);
		if (typePage == 0) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing params");
		}
		String search = request.optString("search");
		if (search == null) search = "";
		
		// Verify we can search the pages. Page 1 is admins and HCPs; page
		// 2 is patients. 
		if (typePage == 1) {
			if (!u.hasAccess(ACE.Administrator)) {
				return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
			}
		} else if (typePage == 2) {
			if (!u.hasAccess(ACE.HealthCareProvider)) {
				return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
			}
		} else {
			return new ReturnResult(Errors.INCORRECTPARAMS,"Wrong parameters");
		}
		

		/**
		 * Perform search, restrict results
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
				
		try {
			// Depending on the page we select based on the access control list
			c = Database.get();
			if (typePage == 1) {
				ps = c.prepareStatement(
						"select users.userid, users.username, users.email, users.name, array_agg(useraccesscontrollist.ace) " +
								"FROM users, useraccesscontrollist " +
								"WHERE users.userid = useraccesscontrollist.userid " +
								"AND users.name like ? " +
								"AND useraccesscontrollist.userid IN ( 1, 2 ) " +
								"GROUP BY users.userid " + 
								"LIMIT 30 ");
			} else {
				ps = c.prepareStatement(
						"select users.userid, users.username, users.email, users.name, array_agg(useraccesscontrollist.ace) " +
								"FROM users, useraccesscontrollist " +
								"WHERE users.userid = useraccesscontrollist.userid " +
								"AND users.name like ? " +
								"AND useraccesscontrollist.userid IN ( 3 ) " +
								"GROUP BY users.userid " + 
								"LIMIT 30 ");
			}
			ps.setString(1, "%" + search + "%");
			rs = ps.executeQuery();

			SearchResult result = new SearchResult();
			while (rs.next()) {
				int userID = rs.getInt(1);
				String username = rs.getString(2);
				String email = rs.getString(3);
				String name = rs.getString(4);
				Array acl = rs.getArray(5);
				Object ldata = acl.getArray();
				Integer[] list = (Integer[])ldata;
				
				result.insertResult(userID,username,email,name,list);
			}
			
			rs.close();
			ps.close();
			
			/*
			 * Get the ACL for this user from the database
			 */

			return result;			
		}
		finally {
			if (c != null) c.close();
			if (ps != null) ps.close();
			if (rs != null) rs.close();
		}
	}
	
	/**
	 * Get user basic info for a specific user, searched by e-mail
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult getBasicInfo(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		if (!u.hasAccess(ACE.Administrator) && !u.hasAccess(ACE.HealthCareProvider)) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		String email = request.optString("email");
		if (email == null) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing param");
		}
		

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
				
		try {
			c = Database.get();
			ps = c.prepareStatement(
					"SELECT userid, username, email, name "
							+ "FROM Users "
							+ "WHERE email = ?");
			ps.setString(1, email);
			rs = ps.executeQuery();

			int userID;
			UserReturnResult result;
			if (rs.next()) {
				userID = rs.getInt(1);
				String username = rs.getString(2);
				String useremail = rs.getString(3);
				String name = rs.getString(4);
				
				result = new UserReturnResult(userID, username, useremail, name);
				
			} else {
				return new ReturnResult(Errors.NOSUCHUSER,"User does not exit");
			}
			
			rs.close();
			ps.close();
			
			/*
			 * Get the ACL for this user from the database
			 */
			
			ps = c.prepareStatement("SELECT ace FROM UserAccessControlList WHERE userid = ?");
			ps.setInt(1, userID);
			rs = ps.executeQuery();
			HashSet<Integer> acl = new HashSet<Integer>();
			while (rs.next()) {
				acl.add(rs.getInt(1));
			}
			
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
	 * Add a new user to the database. Note that this creates the user but
	 * leaves the username blank and instead generates a UUID for onboarding.
	 * This will also trigger an onboarding e-mail message
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult addUser(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		if (!u.hasAccess(ACE.Administrator) && !u.hasAccess(ACE.HealthCareProvider)) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		String email = request.optString("email");
		String name = request.optString("name");
		if ((email == null) || (name == null)) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing param");
		}
		String uuid = UUID.randomUUID().toString();

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
				
		try {
			c = Database.get();
			ps = c.prepareStatement(
					"INSERT INTO Users ( onboardtoken, email, name ) " +
					"VALUES ( ?, ?, ? ); SELECT currval('Users_userid_seq')");
			ps.setString(1, uuid);
			ps.setString(2, email);
			ps.setString(3, name);
        	ps.execute();

            int utc = ps.getUpdateCount();
            int userid = 0;
            if ((utc == 1) && ps.getMoreResults()) {
                rs = ps.getResultSet();
                if (rs.next()) {
                	userid = rs.getInt(1);
                }
                rs.close();
            }
			ps.close();
			ps = null;
			
			/*
			 * Send onboarding e-mail
			 */
			
			String onboardUrl = serverURL + "/onboard.html";
			EMailUtil.sendResetPassword(email, onboardUrl, uuid);
			
			/*
			 * Return the userID
			 */
			
			SimpleReturnResult ret = new SimpleReturnResult("userid",userid);
			return ret;
		}
		finally {
			if (c != null) c.close();
			if (ps != null) ps.close();
			if (rs != null) rs.close();
		}

	}
}
