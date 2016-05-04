/*	UserRecord.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.util;

import java.io.Serializable;

/**
 * Stores session state used for tracking the login status of a user.
 * @author woody
 *
 */
public class UserRecord implements Serializable
{
	private static final long serialVersionUID = 1L;

	private int userID;
	
	public UserRecord(int uid)
	{
		userID = uid;
	}
	
	public int getUserID()
	{
		return userID;
	}
}
