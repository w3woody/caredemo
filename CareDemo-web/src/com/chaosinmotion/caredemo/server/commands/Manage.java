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
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import com.chaosinmotion.caredemo.server.database.Database;
import com.chaosinmotion.caredemo.server.json.ReturnResult;
import com.chaosinmotion.caredemo.server.json.SearchResult;
import com.chaosinmotion.caredemo.server.util.UserRecord;
import com.chaosinmotion.caredemo.shared.ACE;
import com.chaosinmotion.caredemo.shared.Errors;

/**
 * 	Handles requests centered around managing users. This contains the code
 * 	not part of the profile group which allows us to create, remove and modify
 * 	users as well as search for them. This includes:
 * 
 * 		manage/search				-- Search for users
 * 		manage/addUser				-- Add a new user
 * 		manage/removeUser			-- 
 * 
 * @author woody
 *
 */
public class Manage
{
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
						"select users.userid, users.email, users.name, array_agg(useraccesscontrollist.ace) " +
								"FROM users, useraccesscontrollist " +
								"WHERE users.userid = useraccesscontrollist.userid " +
								"AND users.name like ? " +
								"AND useraccesscontrollist.userid IN ( 1, 2 ) " +
								"GROUP BY users.userid " + 
								"LIMIT 30 ");
			} else {
				ps = c.prepareStatement(
						"select users.userid, users.email, users.name, array_agg(useraccesscontrollist.ace) " +
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
				String email = rs.getString(2);
				String name = rs.getString(3);
				Array acl = rs.getArray(4);
				Object ldata = acl.getArray();
				Integer[] list = (Integer[])ldata;
				
				result.insertResult(userID,email,name,list);
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
}
