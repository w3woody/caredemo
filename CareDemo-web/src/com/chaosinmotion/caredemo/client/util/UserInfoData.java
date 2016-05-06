/*	UserInfoData.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.util;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author woody
 *
 */
public class UserInfoData
{
	private int userid;
	private String email;
	private String name;
	private HashSet<Integer> acl;
	
	UserInfoData(int uid, String em, String n, Collection<Integer> a)
	{
		userid = uid;
		email = em;
		name = n;
		acl = new HashSet<Integer>(a);
	}
	
	public int getUserID()
	{
		return userid;
	}
	
	public String getEMail()
	{
		return email;
	}
	
	public String name()
	{
		return name;
	}
	
	public boolean canAccess(int ace)
	{
		return acl.contains(ace);
	}
}
