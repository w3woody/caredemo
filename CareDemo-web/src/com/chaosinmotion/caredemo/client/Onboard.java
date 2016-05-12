/*	Onboard.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client;

import com.chaosinmotion.caredemo.client.panels.OnboardingPanel;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entrpoint for page which allows a user to onboard himself after receiving
 * an invitation. This page allows the user to pick a username and a password,
 * then logs him in.
 * @author woody
 *
 */
public class Onboard implements EntryPoint
{
	@Override
	public void onModuleLoad()
	{
		String token = Window.Location.getParameter("token");
		OnboardingPanel panel = new OnboardingPanel(token);
		panel.setWidth("100%");
		
		RootPanel rootPanel = RootPanel.get("contentpanel");
		rootPanel.add(panel);
	}
}
