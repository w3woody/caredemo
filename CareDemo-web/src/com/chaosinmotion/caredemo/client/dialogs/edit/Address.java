/*	Address.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs.edit;

import com.google.gwt.json.client.JSONObject;

/**
 * @author woody
 *
 */
public class Address
{
	/**
	 * @param object
	 */
	public Address(JSONObject object)
	{
		addrID = (int)(object.get("index").isNumber().doubleValue());
		name = object.get("name").isString().stringValue();
		addr1 = object.get("addr1").isString().stringValue();
		addr2 = object.get("addr2").isString().stringValue();
		city = object.get("city").isString().stringValue();
		state = object.get("state").isString().stringValue();
		postal = object.get("postal").isString().stringValue();
	}
	
	public Address()
	{
	}
	
	public boolean edit;
	public int addrID;
	public String name;
	public String addr1;
	public String addr2;
	public String city;
	public String state;
	public String postal;
}
