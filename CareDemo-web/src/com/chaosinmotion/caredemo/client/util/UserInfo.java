/*	UserInfo.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.util;

import java.util.HashSet;
import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.network.Network;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;

/**
 * Client side singleton which stores information about the user, including
 * the ACL for the user. This allows me to control which navigation buttons
 * we show. Note that the back end also manages by ACL, so even if we
 * bypass the ACL controls, the back end will simply fail.
 * @author woody
 *
 */
public class UserInfo
{
	/**
	 * Callback for getting user data. Under some cases this can require a
	 * call to the back end, so this is declared asynchronous.
	 * @author woody
	 *
	 */
	public interface Callback
	{
		void userData(UserInfoData userData);
	}
	
	private static UserInfo shared;
	private UserInfoData data;
	
	private UserInfo()
	{
		
	}
	
	public static synchronized UserInfo get()
	{
		if (shared == null) {
			shared = new UserInfo();
		}
		return shared;
	}
	
	/**
	 * Given the JSON response object from the server, this sets the
	 * internal fields for this user.
	 * @param serverUserInfo
	 */
	public void setUserInfo(JSONObject serverUserInfo)
	{
		try {
			String name = serverUserInfo.get("name").isString().stringValue();
			String email = serverUserInfo.get("email").isString().stringValue();
			int userid = (int)(serverUserInfo.get("userid").isNumber().doubleValue());
			
			HashSet<Integer> acl = new HashSet<Integer>();
			JSONArray array = serverUserInfo.get("acl").isArray();
			int i,len = array.size();
			for (i = 0; i < len; ++i) {
				int ace = (int)(array.get(i).isNumber().doubleValue());
				acl.add(ace);
			}
			
			data = new UserInfoData(userid,email,name,acl);
		}
		catch (Throwable err) {
			// We get to here if there was a problem with the object being
			// loaded. We zero out the storage (if present), then bounce
			// the user to the login page, because there is little beyond
			// that which I can do.
			Storage storage = Storage.getSessionStorageIfSupported();
			if (storage != null) {
				storage.removeItem("user");
			}
			Window.Location.replace("index.html");
		}
	}
	
	/**
	 * Call this if the basic information is reloaded
	 */
	public void clearCache()
	{
		data = null;
		Storage storage = Storage.getSessionStorageIfSupported();
		if (storage != null) {
			storage.removeItem("user");
		}
	}
	
	/**
	 * Get the user info. This will return the use info stored internally,
	 * or stored in the session, or (if that is not available) by calling
	 * the back end for the user info.
	 * @param callback
	 */
	public void getUserInfo(final Callback callback)
	{
		if (data != null) {
			callback.userData(data);
			return;
		}
		
		Storage storage = Storage.getSessionStorageIfSupported();
		if (storage != null) {
			String userData = storage.getItem("user");
			if (userData != null) {
				JSONObject u = JSONParser.parseLenient(userData).isObject();
				setUserInfo(u);
				if (data != null) {
					callback.userData(data);
					return;
				}
			}
		}
		
		/*
		 * 	If we get here the user data from the back end
		 */
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("users/userinfo"));
		
		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				/*
				 * Stash away the login information in persistant store
				 * and return success.
				 */
				
				JSONObject userInfo = result.get("data").isObject();
				Storage storage = Storage.getSessionStorageIfSupported();
				if (storage != null) {
					storage.setItem("user", userInfo.toString());
				}
				setUserInfo(userInfo);
				
				callback.userData(data);
			}
			
			@Override
			public void error(int serverError)
			{
				new MessageBox("Error","An unknown network problem occurred.");
			}
		});
	}
	
}
