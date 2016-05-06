/*	SecureChat: A secure chat system which permits secure communications 
 *  between iOS devices and a back-end server.
 *
 *	Copyright © 2016 by William Edward Woody
 *
 *	This program is free software: you can redistribute it and/or modify it 
 *	under the terms of the GNU General Public License as published by the 
 *	Free Software Foundation, either version 3 of the License, or (at your 
 *	option) any later version.
 *
 *	This program is distributed in the hope that it will be useful, but 
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *	for more details.
 *
 *	You should have received a copy of the GNU General Public License along 
 *	with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.chaosinmotion.caredemo.server.json;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Base class used to formulate a return result from the SecureChat server.
 * All return results that return a complex value must descend from this
 * class.
 * 
 * @author woody
 *
 */
public class ReturnResult
{
	private boolean success;
	private int errorCode;
	private String errorMessage;
	private List<String> exception;
	
	/**
	 * Generates a successful return result 
	 * @param success
	 */
	public ReturnResult()
	{
		success = true;
	}
	
	/**
	 * Generate an error return result. The error code is a computer-readable
	 * error message which can then be interpreted by the client and
	 * translated into an appropriate error response. The message is sent
	 * to assist in development and debugging.
	 * @param error
	 * @param message
	 */
	public ReturnResult(int error, String message)
	{
		success = false;
		errorCode = error;
		errorMessage = message;
		exception = null;
	}
	
	/**
	 * Any child of this class must inherit from this class. This gives any
	 * child class the opportunity to format a data field that can be returned
	 * as part of the return code. If this returns null, a data field is
	 * not added.
	 * @return
	 */
	public JSONObject returnData()
	{
		return null;
	}
	
	/**
	 * Convert to string. If this is successful the return looks like:
	 * { "success": true, "data": ... }
	 * If an error, this looks like:
	 * { "success": false, "error": number, "message": "msg", "exception": [...] }
	 */
	public String toString()
	{
		JSONObject obj = new JSONObject();
		
		obj.put("success", success);
		if (success) {
			/*
			 * Generate the success return.
			 */
			JSONObject data = returnData();
			if (data != null) {
				obj.put("data", data);
			}
		} else {
			/*
			 * Generate the error return
			 */
			
			obj.put("error", errorCode);
			obj.put("message", errorMessage);
			if (exception != null) {
				JSONArray array = new JSONArray();
				for (String str: exception) {
					array.put(str);
				}
				obj.put("exception", array);
			}
		}
		
		return obj.toString(4);		// formatted string.
	}
}
