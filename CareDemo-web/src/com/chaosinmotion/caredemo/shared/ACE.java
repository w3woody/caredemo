/*	ACE.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.shared;

/**
 * Access control entries. These indexes must correspond to the constants
 * inserted in the ACE of the database.
 * @author woody
 *
 */
public interface ACE
{
	public int Administrator = 1;
	public int HealthCareProvider = 2;
	public int Patient = 3;
}
