/*	Users.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client;

import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.panels.UserPanel;
import com.chaosinmotion.caredemo.client.util.UserInfo;
import com.chaosinmotion.caredemo.client.util.UserInfoData;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author woody
 *
 */
public class Users implements EntryPoint
{
	private UserPanel userPanel;

	@Override
	public void onModuleLoad()
	{
		/*
		 * Get user info to figure out what we can load.
		 */
		UserInfo.get().getUserInfo(new UserInfo.Callback() {
			@Override
			public void userData(UserInfoData userData)
			{
				loadToolbar(userData);
				loadContents(userData);
			}
		});
	}

	/**
	 * Load contents
	 * @param userData
	 */
	private void loadContents(UserInfoData userData)
	{
		RootPanel panel = RootPanel.get("contentpanel");

		String p = Window.Location.getParameter("p");
		if (p == null) {
			// Missing parameter
			Window.Location.replace("home.html");
			return;
		}
		
		userPanel = new UserPanel(p);
		userPanel.setWidth("100%");
		panel.add(userPanel);
		// TODO: Load table
	}
	
	/**
	 * Load the toolbar
	 * @param userData
	 */
	private void loadToolbar(UserInfoData userData)
	{
		RootPanel toolPanel = RootPanel.get("toolspanel");
		
		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.setSpacing(0);
		toolPanel.add(hpanel);
		
		BarButton profile = new BarButton("Welcome " + userData.name() + ": My Profile");
		hpanel.add(profile);
		profile.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doEditProfile();
			}
		});

		BarButton logout = new BarButton("Logout");
		hpanel.add(logout);
		logout.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doLogout();
			}
		});
	}
	/**
	 * Edit the user's profile. 
	 */
	private void doEditProfile()
	{
		Window.Location.assign("profile.html");
	}
	
	/**
	 * Log the user out.
	 */
	private void doLogout()
	{
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("users/logout"));
		
		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				Window.Location.assign("index.html");
			}
			
			@Override
			public void error(int serverError)
			{
				new MessageBox("Error","An unknown network problem occurred.",new MessageBox.Callback() {
					@Override
					public void finished()
					{
						Window.Location.assign("index.html");
					}
				});
			}
		});
	}
}
