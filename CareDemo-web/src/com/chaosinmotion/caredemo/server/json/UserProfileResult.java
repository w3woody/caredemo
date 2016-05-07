/*	UserProfileResult.java
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
public class UserProfileResult extends ReturnResult
{
	private JSONArray addrs;
	private JSONArray phones;
	
	public UserProfileResult()
	{
		addrs = new JSONArray();
		phones = new JSONArray();
	}
	
	public void addPhone(int index, String name, String phone)
	{
		JSONObject obj = new JSONObject();
		obj.put("index", index);
		obj.put("name", name);
		obj.put("phone", phone);
		phones.put(obj);
	}
	
	public void addAddress(int index, String name, String addr1, String addr2, String city, String state, String postal)
	{
		JSONObject obj = new JSONObject();
		obj.put("index", index);
		obj.put("name", name);
		obj.put("addr1", addr1);
		obj.put("addr2", addr2);
		obj.put("city", city);
		obj.put("state", state);
		obj.put("postal", postal);
		addrs.put(obj);
	}
	
	public JSONObject returnData()
	{
		JSONObject obj = new JSONObject();
		obj.put("addresses", addrs);
		obj.put("phones",phones);
		return obj;
	}
}
