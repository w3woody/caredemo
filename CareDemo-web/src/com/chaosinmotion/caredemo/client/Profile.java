/*	HomePage.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client;

import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.panels.ResetPasswordPanel;
import com.chaosinmotion.caredemo.client.panels.UserAddressPanel;
import com.chaosinmotion.caredemo.client.panels.UserInfoPanel;
import com.chaosinmotion.caredemo.client.panels.UserPhonePanel;
import com.chaosinmotion.caredemo.client.util.UserInfo;
import com.chaosinmotion.caredemo.client.util.UserInfoData;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author woody
 *
 */
public class Profile implements EntryPoint
{
	private UserAddressPanel uapanel;
	private UserPhonePanel uppanel;
	
	@Override
	public void onModuleLoad()
	{
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

		/*
		 * Load basic info panel
		 */
		UserInfoPanel ppanel = new UserInfoPanel();
		ppanel.setWidth("100%");
		panel.add(ppanel);
		
		/*
		 * Address panel
		 */
		
		uapanel = new UserAddressPanel(new UserAddressPanel.Callback() {
			@Override
			public void refresh()
			{
				reloadContents();
			}
		});
		uapanel.setWidth("100%");
		panel.add(uapanel);
		
		/*
		 * Phone number panel
		 */
		
		uppanel = new UserPhonePanel(new UserPhonePanel.Callback() {
			@Override
			public void refresh()
			{
				reloadContents();
			}
		});
		uppanel.setWidth("100%");
		panel.add(uppanel);
		
		/*
		 * Password panel
		 */
		
		ResetPasswordPanel rpanel = new ResetPasswordPanel();
		rpanel.setWidth("100%");
		panel.add(rpanel);
		
		/*
		 * Trigger request to load contents
		 */
		
		reloadContents();
	}
	
	private void reloadContents()
	{
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("profile/getFullProfile"));
		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				JSONObject data = result.get("data").isObject();
				
				JSONArray array = data.get("addresses").isArray();
				uapanel.initialize(array);
				
				array = data.get("phones").isArray();
				uppanel.initialize(array);
			}
			
			@Override
			public void error(int serverError)
			{
				new MessageBox("Network error","A problem occurred loading the contents");
			}
		});
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
		
		BarButton home = new BarButton("Home");
		hpanel.add(home);
		home.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				Window.Location.assign("home.html");
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
