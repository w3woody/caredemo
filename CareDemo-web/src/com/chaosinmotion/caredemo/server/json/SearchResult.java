/*	SearchResult.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.json;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author woody
 *
 */
public class SearchResult extends ReturnResult
{
	private JSONArray results = new JSONArray();

	/**
	 * @param userID
	 * @param email
	 * @param name
	 * @param list
	 */
	public void insertResult(int userID, String email, String name, Integer[] acl)
	{
		JSONObject obj = new JSONObject();
		obj.put("userid", userID);
		obj.put("email", email);
		obj.put("name", name);
		
		JSONArray a = new JSONArray();
		for (Integer i: acl) {
			a.put(i);
		}
		obj.put("acl", a);
		
		results.put(obj);
	}
	
	public JSONObject returnData()
	{
		JSONObject obj = new JSONObject();
		obj.put("results", results);
		return obj;
	}
}
