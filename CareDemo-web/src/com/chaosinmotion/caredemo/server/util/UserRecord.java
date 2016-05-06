/*	UserRecord.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Stores session state used for tracking the login status of a user.
 * @author woody
 *
 */
public class UserRecord implements Serializable
{
	private static final long serialVersionUID = 1L;

	private int userID;
	private HashSet<Integer> acl;
	
	public UserRecord(int uid, Collection<Integer> a)
	{
		userID = uid;
		acl = new HashSet<Integer>(a);
	}
	
	public int getUserID()
	{
		return userID;
	}
	
	public boolean hasAccess(int ace)
	{
		return acl.contains(ace);
	}

	/**
	 * @return
	 */
	public Collection<Integer> getACL()
	{
		return acl;
	}
}
