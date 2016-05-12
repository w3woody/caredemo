/*	Phone.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs.edit;

import com.google.gwt.json.client.JSONObject;

/**
 * @author woody
 *
 */
public class Phone
{
	public Phone()
	{
	}

	/**
	 * @param object
	 */
	public Phone(JSONObject object)
	{
		phoneID = (int)(object.get("index").isNumber().doubleValue());
		name = object.get("name").isString().stringValue();
		phone = object.get("phone").isString().stringValue();
	}
	
	public boolean edit;
	public int phoneID;
	public String name;
	public String phone;
}
