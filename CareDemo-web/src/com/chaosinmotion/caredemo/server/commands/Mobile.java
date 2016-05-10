/*	Mobile.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.commands;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import com.chaosinmotion.caredemo.server.database.Database;
import com.chaosinmotion.caredemo.server.json.DevicesReturnResult;
import com.chaosinmotion.caredemo.server.json.ReturnResult;
import com.chaosinmotion.caredemo.server.json.SimpleReturnResult;
import com.chaosinmotion.caredemo.server.json.UserReturnResult;
import com.chaosinmotion.caredemo.server.util.UserRecord;
import com.chaosinmotion.caredemo.shared.Errors;

/**
 * 	Handles all requests centered around mobile logging in, including the
 * 	following:
 * 
 * 		mobile/getConnectToken		-- Get a token for connecting to the server
 * 		mobile/pollConnection		-- Ask if the user has acknowledged device
 * 		mobile/login				-- Login with user token from device	
 * 		mobile/connect				-- Connect the mobile device to the user
 * 		mobile/devices				-- Get list of devices for this account
 * 		mobile/removeDevice			-- Remove device from list of devices		
 * 
 * @author woody
 *
 */
public class Mobile
{
	/**
	 * Provide a map from the 8 character entry for a device to the user ID
	 * that this is associated with.
	 * 
	 * TODO: This needs to be refactored if we are on an implementation with
	 * multiple servers.
	 */
	private static HashMap<String,UserRecord> connectMap = new HashMap<String,UserRecord>();
	private static HashMap<String,Long> mobileKeys = new HashMap<String,Long>();
	private static SecureRandom random = new SecureRandom();
	
	private static final char[] charMap = {
			'2', '3', '4', '5',
			'6', '7', '8', '9',
			'A', 'B', 'C', 'D',
			'E', 'F', 'G', 'H',
			'J', 'K', 'L', 'M',
			'N', 'P', 'R', 'S',
			'T', 'U', 'V', 'W',
			'X', 'Y', 'Z', 'O'
	};
	
	private static char toHex(int c)
	{
		c &= 0x0F;
		if ((c >= 0) && (c <= 9)) return (char) ('0' + c);
		return (char) (c + 'A' - 10);
	}
	
	private static void purgeOldKeys()
	{
		synchronized(mobileKeys) {
			long ts = System.currentTimeMillis();
			HashSet<String> remove = new HashSet<String>();
			for (Map.Entry<String, Long> me: mobileKeys.entrySet()) {
				if (me.getValue() < ts) {
					remove.add(me.getKey());
				}
			}
			for (String str: remove) {
				mobileKeys.remove(str);
			}
		}
	}
	
	/**
	 * Get the connection token. This returns an 8 digit/character symbol
	 * which can be used by the user to connect to an account.
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult getConnectToken(String cmd, JSONObject request, HttpSession session)
	{
		purgeOldKeys();
		
		String key;
		
		for (;;) {
			int randval;
			synchronized(random) {
				randval = random.nextInt();
			}
			
			byte b = (byte)randval;
			b ^= (byte)(randval >> 8);
			b ^= (byte)(randval >> 16);
			b ^= (byte)(randval >> 24);
			if (b == 0) b = (byte)0xFF;
			
			long l = 0x00FFFFFFFF & randval;
			l = (l << 8) | (0x00FF & b);
			
			/*
			 * Generate string
			 */
			
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < 8; ++i) {
				int index = 0x1F & (int)l;
				l >>>= 5;
				builder.append(charMap[index]);
			}
			
			key = builder.toString();
			synchronized(mobileKeys) {
				if (!mobileKeys.containsKey(key)) break;
			}
		}
		
		synchronized(mobileKeys) {
			long ts = System.currentTimeMillis() + 600000L;	// 10 minutes
			mobileKeys.put(key, ts);
		}
		
		session.setAttribute("mobile", key);
		
		return new SimpleReturnResult("token",key);
	}
	
	/**
	 * This API should be called when the user, from the web page, enters
	 * the token that was sent to the device. This formats the token, verifies
	 * it is a valid token, and adds it to the list of tokens that are 
	 * onboarded for this user.
	 * 
	 * This allows us to work with pollConnection in order to verify the
	 * mobile device is now connected.
	 * 
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 */
	public ReturnResult connect(String cmd, JSONObject request, HttpSession session) 
	{
		String key = request.optString("key");
		if (key == null) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing Param");
		}

		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		
		/*
		 * Step 1: Format the entered string
		 */
		
		key = key.toUpperCase();
		char[] cl = key.toCharArray();
		if (cl.length != 8) {
			return new ReturnResult(Errors.INCORRECTMOBILEKEY,"Wrong key");
		}
		for (int i = 0; i < cl.length; ++i) {
			char c = cl[i];
			if (c == '0') {
				cl[i] = 'O';
			}
		}
		key = new String(cl);		// normalized key.
		
		/*
		 * Step 2: Determine if someone is waiting for this key
		 */
		
		purgeOldKeys();
		synchronized(mobileKeys) {
			if (!mobileKeys.containsKey(key)) {
				return new ReturnResult(Errors.INCORRECTMOBILEKEY,"Wrong key");
			}
			mobileKeys.remove(key);
			connectMap.put(key, u);
		}
		
		return new ReturnResult();
	}
	
	/**
	 * Poll the connection. This examines the token associated with this 
	 * mobile device to see if the user has entered the key via connect.
	 * 
	 * This is called periodically by the mobile device. When successful
	 * (because the user entered the token displayed on the device), this
	 * completes the transactions and sends a login token to the device for
	 * future reference.
	 */
	public ReturnResult pollConnection(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		purgeOldKeys();

		String devType = request.optString("device");		// device identifier
		if (devType == null) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing Param");
		}
		
		String key = (String)session.getAttribute("mobile");
		if (key == null) {
			return new ReturnResult(Errors.MOBILEEXPIREDCONNECT,"Expired credentials");
		}
		
		UserRecord u;
		synchronized(mobileKeys) {
			if (!mobileKeys.containsKey(key)) {
				// Key expired.
				return new ReturnResult(Errors.MOBILEEXPIREDCONNECT,"Expired key");
			}
			
			if (!connectMap.containsKey(key)) {
				// Key not entered yet.
				return new SimpleReturnResult("connected",false);
			}
			
			u = connectMap.get(key);
		}
		
		/*
		 * Generate a login token for this device. Pull the information
		 * that was given to us
		 */
		
		String t;			// generated token
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			ps = c.prepareStatement("SELECT COUNT(*) FROM MobileDevices WHERE token = ?");
			for (;;) {
				// Generate random 64-character token.
				byte[] token = new byte[32];
				synchronized(random) {
					random.nextBytes(token);
				}
				StringBuffer buffer = new StringBuffer();
				for (byte b: token) {
					buffer.append(toHex(b >> 4)).append(toHex(b));
				}
				t = buffer.toString();
				
				// Verify we didn't collide.
				ps.setString(1, t);
				rs = ps.executeQuery();
				int ct = 0;
				if (rs.next()) {
					ct = rs.getInt(1);
				}
				if (ct == 0) {
					break;
				}
				rs.close();
				rs = null;
			}
			
			// At this point we have a token for logging in. Insert into
			// our database
			ps.close();
			ps = null;
			
			ps = c.prepareStatement("INSERT INTO MobileDevices " +
								    "    ( userid, token, description ) " +
								    "VALUES " +
								    "    ( ?, ?, ? )");
			ps.setInt(1, u.getUserID());
			ps.setString(2, t);
			ps.setString(3, devType);
			
			ps.execute();
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
		
		/*
		 * At this point we have a token t. Return. Note that we do not log
		 * the mobile device in at this point.
		 */
		
		SimpleReturnResult ret = new SimpleReturnResult("connected",true);
		ret.put("token",t);
		return ret;
	}
	
	/**
	 * Handle mobile device login by verifying the supplied token matches.
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
		String token = request.optString("token");
		if (token == null) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing Param");
		}
		
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = Database.get();
			ps = c.prepareStatement("SELECT Users.userid, Users.username, Users.email, Users.name " +
							   		"FROM Users, MobileDevices " +
							   		"WHERE Users.userid = MobileDevices.userid " +
							   		"AND MobileDevices.token = ?");
			ps.setString(1, token);
			rs = ps.executeQuery();
			int userID;
			UserReturnResult result;
			if (rs.next()) {
				userID = rs.getInt(1);
				String username = rs.getString(2);
				String email = rs.getString(3);
				String name = rs.getString(4);
				
				result = new UserReturnResult(userID, username, email, name);
				
			} else {
				return new ReturnResult(Errors.INCORRECTCREDENTIALS,"Wrong token");
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
	 * Returns a list of devices associated with this user
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult devices(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
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
			ps = c.prepareStatement("SELECT deviceid, description " +
									"FROM MobileDevices " +
									"WHERE userid = ?");
			ps.setInt(1, u.getUserID());
			rs = ps.executeQuery();
			DevicesReturnResult dr = new DevicesReturnResult();
			while (rs.next()) {
				int deviceID = rs.getInt(1);
				String description = rs.getString(2);
				dr.addDevice(deviceID, description);
			}
			return dr;
		}
		finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
	
	/**
	 * Remove a device. Requires the user to be logged in, and only
	 * removes devices associated with the user.
	 * @param cmd
	 * @param request
	 * @param session
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws IOException
	 */
	public ReturnResult removeDevice(String cmd, JSONObject request, HttpSession session) throws ClassNotFoundException, SQLException, IOException
	{
		UserRecord u = (UserRecord)session.getAttribute("userid");
		if (u == null) {
			return new ReturnResult(Errors.NOTLOGGEDIN,"Not logged in");
		}
		
		int deviceID = request.optInt("deviceid", 0);
		if (deviceID == 0) {
			return new ReturnResult(Errors.MISSINGPARAM,"Missing Param");
		}

		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = Database.get();
			ps = c.prepareStatement("DELETE FROM MobileDevices " +
								    "WHERE userid = ? " +
								    "AND deviceid = ?");
			ps.setInt(1, u.getUserID());
			ps.setInt(2, deviceID);
			ps.execute();
			
			return new ReturnResult();
		}
		finally {
			if (ps != null) ps.close();
			if (c != null) c.close();
		}
	}
}
