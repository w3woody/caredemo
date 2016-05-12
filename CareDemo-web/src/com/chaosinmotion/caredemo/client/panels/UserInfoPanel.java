/*	UserInfoPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.util.UserInfo;
import com.chaosinmotion.caredemo.client.util.UserInfoData;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.client.widgets.DialogWidget;
import com.chaosinmotion.caredemo.shared.Errors;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author woody
 *
 */
public class UserInfoPanel extends ContentPanel
{
	private TextBox name;
	private TextBox email;

	/**
	 * @param title
	 */
	public UserInfoPanel()
	{
		super("Basic Information");
		setContent(initWidget());
	}

	protected Widget initWidget()
	{
		DialogWidget table = new DialogWidget();
		
		/*
		 * Populate the name, e-mail, and provide buttons for saving
		 */
		
		name = table.addTextBox(0, "Name:");
		email = table.addTextBox(1, "E-Mail:");
		
		HorizontalPanel hpanel = table.addButtonPanel(2);
				
		BarButton barButton = new BarButton("Save");
		hpanel.add(barButton);
		barButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doUpdateBasicInfo();
			}
		});
		
		UserInfo.get().getUserInfo(new UserInfo.Callback() {
			@Override
			public void userData(UserInfoData userData)
			{
				name.setText(userData.name());
				email.setText(userData.getEMail());
			}
		});
		
		return table;
	}
	
	private void doUpdateBasicInfo()
	{
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("profile/updateBasicInfo"));
		req.put("name", new JSONString(name.getText()));
		req.put("email", new JSONString(email.getText()));
		
		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				new MessageBox("Success","Your basic information has been udpated.");
				UserInfo.get().clearCache();
			}
			
			@Override
			public void error(int serverError)
			{
				if (serverError == Errors.ACCESSVIOLATION) {
					new MessageBox("Access Violation","Unable to performt this operation");
				} else {
					new MessageBox("Error","Unable to performt this operation; e-mail address already used?");
				}
			}
		});
	}
}
