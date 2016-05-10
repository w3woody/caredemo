/*	UserReturnResult.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.json;

import java.util.Collection;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author woody
 *
 */
public class UserReturnResult extends ReturnResult
{
	private int userID;
	private String username;
	private String email;
	private String name;
	private Collection<Integer> acl;
	
	/**
	 * Generate a return result for this user
	 * @param uid UserID
	 * @param em Email
	 * @param n Name
	 */
	public UserReturnResult(int uid, String un, String em, String n)
	{
		username = un;
		userID = uid;
		email = em;
		name = n;
	}
	
	/**
	 * Set the ACL for this user
	 * @param m
	 */
	public void setACL(Collection<Integer> m)
	{
		acl = m;
	}
	
	public JSONObject returnData()
	{
		JSONObject obj = new JSONObject();
		obj.put("username", username);
		obj.put("userid", userID);
		obj.put("email", email);
		obj.put("name", name);
		
		JSONArray a = new JSONArray();
		for (Integer i: acl) {
			a.put(i.intValue());
		}
		obj.put("acl", a);
		
		return obj;
	}

}
