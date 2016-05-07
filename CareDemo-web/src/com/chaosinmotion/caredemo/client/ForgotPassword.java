/*	ForgotPassword.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client;

import com.chaosinmotion.caredemo.client.dialogs.ResetPassword;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;

/**
 * @author woody
 *
 */
public class ForgotPassword implements EntryPoint
{
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad()
	{
		// TODO: Extract token, pass to reset
		String token = Window.Location.getParameter("token");
		if (token == null) {
			Window.Location.replace("index.html");
			return;
		}
		
		new ResetPassword(token,new ResetPassword.Callback() {
			@Override
			public void success()
			{
				Window.Location.assign("home.html");
			}
			
			@Override
			public void failure()
			{
				Window.Location.assign("index.html");
			}
		});
	}
}
