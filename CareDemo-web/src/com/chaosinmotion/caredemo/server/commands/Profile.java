/*	Profile.java
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
import com.chaosinmotion.caredemo.server.json.SimpleReturnResult;
import com.chaosinmotion.caredemo.server.json.UserProfileResult;
import com.chaosinmotion.caredemo.server.json.UserReturnResult;
import com.chaosinmotion.caredemo.server.util.UserRecord;
import com.chaosinmotion.caredemo.shared.ACE;
import com.chaosinmotion.caredemo.shared.Errors;

/**
 * 	Handles all requests centered around managing a user profile. This
 * 	includes:
 * 
 * 		profile/updateBasicInfo		-- Update profile name, email address
 * 		profile/getFullProfile		-- Get complex structure with all user info
 * 		profile/addAddress			-- Add an address record
 * 		profile/updateAddress		-- Update address record
 * 		profile/removeAddress		-- Remove address record
 * 		profile/addPhone			-- Add a phone number
 * 		profile/updatePhone			-- Update phone number
 * 		profile/removePhone			-- Remove phone number
 * 
 * 	Note: in order to access any of these methods you must either be the user
 * 	specified in the record, or you must be an administrator.
 */
public class Profile
{
	/**
	 * Either returns the user ID, or 0 if the user ID cannot be edited
	 * @param req
	 * @return
	 */
	private int getUserID(JSONObject req, UserRecord u)
	{
		int userID = req.optInt("userid", 0);
		if (userID == 0) {
			return u.getUserID();
		} else if (userID == u.getUserID()) {
			return userID;
		} else if (!u.hasAccess(ACE.Administrator)) {
			return 0;
		} else {
			return userID;
		}
	}

	/**
	 * Get user basic info for a specific user.
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
		int userID = getUserID(request,u);
		if (userID == 0) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
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
			ps.setInt(1, userID);
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
	

	public ReturnResult updateBasicInfo(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		int userID = getUserID(request,u);
		if (userID == 0) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		/*
		 * Pull parameters
		 */
		
		String name = request.optString("name");
		String email = request.optString("email");
		if ((name == null) || (email == null)) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing params");
		}
		
		/*
		 * Update user information
		 */
		
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = Database.get();
			ps = c.prepareStatement("UPDATE Users " +
									"SET email = ?, " + 
									"    name = ? " +
									"WHERE userid = ?");
			
			// Note that this can fail if the e-mail address is already set.
			// We could handle that as a special case, but instead we just
			// dump an exception.
			
			ps.setString(1, email);
			ps.setString(2, name);
			ps.setInt(3, userID);
			ps.execute();

			return new ReturnResult();
		}
		finally {
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
	
	/**
	 * Get the full profile. This returns everything except the basic info
	 * for this user.
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult getFullProfile(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		int userID = getUserID(request,u);
		if (userID == 0) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		/*
		 * Derive full info for this user
		 */
		
		UserProfileResult res = new UserProfileResult();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			
			// Addresses
			ps = c.prepareStatement("SELECT addrid, name, addr1, addr2, city, state, postalcode " +
									"FROM UserAddress " + 
									"WHERE userid = ?");
			ps.setInt(1, userID);
			
			rs = ps.executeQuery();
			while (rs.next()) {
				int index = rs.getInt(1);
				String name = rs.getString(2);
				String addr1 = rs.getString(3);
				String addr2 = rs.getString(4);
				String city = rs.getString(5);
				String state = rs.getString(6);
				String postalcode = rs.getString(7);
				
				res.addAddress(index, name, addr1, addr2, city, state, postalcode);
			}
			rs.close();
			rs = null;
			ps.close();
			ps = null;
			
			// Phone numbers
			ps = c.prepareStatement("SELECT phoneid, name, phone " +
									"FROM UserPhone " + 
									"WHERE userid = ?");
			ps.setInt(1, userID);
			
			rs = ps.executeQuery();
			while (rs.next()) {
				int index = rs.getInt(1);
				String name = rs.getString(2);
				String phone = rs.getString(3);
				
				res.addPhone(index, name, phone);
			}

			return res;
		}
		
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
	
	public ReturnResult addAddress(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		int userID = getUserID(request,u);
		if (userID == 0) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		/*
		 * Get parameters
		 */
		
		String name = request.optString("name");
		String addr1 = request.optString("addr1");
		String addr2 = request.optString("addr2");
		String city = request.optString("city");
		String state = request.optString("state");
		String postal = request.optString("postal");
		
		if ((name == null) || (addr1 == null) || (addr2 == null) ||
				(city == null) || (state == null) || (postal == null)) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing params");
		}

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			
			// Addresses
			ps = c.prepareStatement("INSERT INTO UserAddress " +
									"    ( userid, name, addr1, addr2, city, state, postalcode ) " +
									"VALUES " +
									"	 ( ?, ?, ?, ?, ?, ?, ? ); SELECT currval('UserAddress_addrid_seq')");

			ps.setInt(1, userID);
			ps.setString(2, name);
			ps.setString(3, addr1);
			ps.setString(4, addr2);
			ps.setString(5, city);
			ps.setString(6, state);
			ps.setString(7, postal);
			
        	ps.execute();
        	
            int utc = ps.getUpdateCount();
            int addrid = 0;
            if ((utc == 1) && ps.getMoreResults()) {
                rs = ps.getResultSet();
                if (rs.next()) {
                	addrid = rs.getInt(1);
                }
                rs.close();
            }
			ps.close();
			ps = null;
			
			return new SimpleReturnResult("index",addrid);
		}
		
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
	
	public ReturnResult removeAddress(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		int userID = getUserID(request,u);
		if (userID == 0) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		int index = request.optInt("index",0);
		if (index == 0) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing params");
		}
		
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = Database.get();
			
			// Addresses
			ps = c.prepareStatement("DELETE FROM UserAddress " +
									"WHERE userid = ? AND addrid = ?");

			ps.setInt(1, userID);
			ps.setInt(2, index);
			
        	ps.execute();
        	
        	return new ReturnResult();
		}
		
		finally {
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
	
	public ReturnResult updateAddress(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		int userID = getUserID(request,u);
		if (userID == 0) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		/*
		 * Get parameters
		 */
		
		int index = request.optInt("index",0);
		String name = request.optString("name");
		String addr1 = request.optString("addr1");
		String addr2 = request.optString("addr2");
		String city = request.optString("city");
		String state = request.optString("state");
		String postal = request.optString("postal");
		
		if ((index == 0) || (name == null) || (addr1 == null) || (addr2 == null) ||
				(city == null) || (state == null) || (postal == null)) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing params");
		}

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			
			// Addresses
			ps = c.prepareStatement("UPDATE UserAddress " +
									"SET name = ?, " +
									"    addr1 = ?, " +
									"    addr2 = ?, " +
									"    city = ?, " +
									"    state = ?, " +
									"    postalcode = ? " +
									"WHERE addrid = ? AND userid = ?");
			
			ps.setString(1, name);
			ps.setString(2, addr1);
			ps.setString(3, addr2);
			ps.setString(4, city);
			ps.setString(5, state);
			ps.setString(6, postal);
			ps.setInt(7, index);
			ps.setInt(8, userID);

        	ps.execute();
        	
        	return new ReturnResult();
		}
		
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
	
	public ReturnResult addPhone(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		int userID = getUserID(request,u);
		if (userID == 0) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		/*
		 * Get parameters
		 */
		
		String name = request.optString("name");
		String phone = request.optString("phone");
		
		if ((name == null) || (phone == null)) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing params");
		}

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			
			// Addresses
			ps = c.prepareStatement("INSERT INTO UserPhone " +
									"    ( userid, name, phone ) " +
									"VALUES " +
									"	 ( ?, ?, ? ); SELECT currval('UserPhone_phoneid_seq')");

			ps.setInt(1, userID);
			ps.setString(2, name);
			ps.setString(3, phone);
			
        	ps.execute();
        	
            int utc = ps.getUpdateCount();
            int phoneid = 0;
            if ((utc == 1) && ps.getMoreResults()) {
                rs = ps.getResultSet();
                if (rs.next()) {
                	phoneid = rs.getInt(1);
                }
                rs.close();
            }
			ps.close();
			ps = null;
			
			return new SimpleReturnResult("index",phoneid);
		}
		
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
	
	public ReturnResult removePhone(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		int userID = getUserID(request,u);
		if (userID == 0) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		int index = request.optInt("index",0);
		if (index == 0) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing params");
		}
		
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = Database.get();
			
			// Addresses
			ps = c.prepareStatement("DELETE FROM UserPhone " +
									"WHERE userid = ? AND phoneid = ?");

			ps.setInt(1, userID);
			ps.setInt(2, index);
			
        	ps.execute();
        	
        	return new ReturnResult();
		}
		
		finally {
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
	
	public ReturnResult updatePhone(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		/*
		 * Standard preamble
		 */
		
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		int userID = getUserID(request,u);
		if (userID == 0) {
			return new ReturnResult(Errors.ACCESSVIOLATION,"Access violation");
		}
		
		/*
		 * Get parameters
		 */
		
		int index = request.optInt("index",0);
		String name = request.optString("name");
		String phone = request.optString("phone");
		
		if ((index == 0) || (name == null) || (phone == null)) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing params");
		}

		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			
			// Addresses
			ps = c.prepareStatement("UPDATE UserPhone " +
									"SET name = ?, " +
									"    phone = ? " +
									"WHERE phoneid = ? AND userid = ?");
			
			ps.setString(1, name);
			ps.setString(2, phone);
			ps.setInt(3, index);
			ps.setInt(4, userID);

        	ps.execute();
        	
        	return new ReturnResult();
		}
		
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
}
