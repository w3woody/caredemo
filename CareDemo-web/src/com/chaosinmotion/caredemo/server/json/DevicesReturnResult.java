/*	DevicesReturnResult.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.json;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Return a list of devices that are currently associated with this user.
 * This gives the user the option to remove a device from the list of
 * devices that are currently able to log into his account.
 * @author woody
 *
 */
public class DevicesReturnResult extends ReturnResult
{
	private JSONArray list;
	
	public DevicesReturnResult()
	{
		list = new JSONArray();
	}
	
	public void addDevice(int ident, String desc)
	{
		JSONObject obj = new JSONObject();
		obj.put("deviceid",ident);
		obj.put("description",desc);
		list.put(obj);
	}
	
	public JSONObject returnData()
	{
		JSONObject obj = new JSONObject();
		obj.put("devices", list);
		return obj;
	}
}
