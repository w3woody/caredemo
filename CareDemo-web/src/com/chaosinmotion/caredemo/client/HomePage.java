/*	HomePage.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client;

import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.network.Network;
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
public class HomePage implements EntryPoint
{
	@Override
	public void onModuleLoad()
	{
		UserInfo.get().getUserInfo(new UserInfo.Callback() {
			@Override
			public void userData(UserInfoData userData)
			{
				loadToolbar(userData);
				// TODO Auto-generated method stub
				
			}
		});
		
		/*
		 * Determine based on the user info which modules we can show.
		 * The patient module shows summary information for the user and
		 * a welcome message. Admin modules and Health Care professional
		 * modules provide links to separate pages.
		 */
		
		
	}
	
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
				Window.Location.replace("index.html");
			}
			
			@Override
			public void error(int serverError)
			{
				new MessageBox("Error","An unknown network problem occurred.",new MessageBox.Callback() {
					@Override
					public void finished()
					{
						Window.Location.replace("index.html");
					}
				});
			}
		});
	}
}
