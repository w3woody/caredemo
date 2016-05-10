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
import com.chaosinmotion.caredemo.shared.Errors;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
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
		FlexTable table = new FlexTable();
		table.setWidth("100%");
		
		table.setCellPadding(8);
		table.setCellSpacing(0);
		table.setBorderWidth(0);
		
		table.getColumnFormatter().setWidth(0, "120px");
		
		/*
		 * Populate the name, e-mail, and provide buttons for saving
		 */
		
		table.setText(0, 0, "Name:");
		table.getCellFormatter().setStyleName(0, 0, "dialoglabel");
		
		name = new TextBox();
		name.setStyleName("dialogtextbox");
		table.setWidget(0, 1, name);
		
		table.setText(1, 0, "EMail:");
		table.getCellFormatter().setStyleName(1, 0, "dialoglabel");
		
		email = new TextBox();
		email.setStyleName("dialogtextbox");
		table.setWidget(1, 1, email);
		
		table.getFlexCellFormatter().setColSpan(2, 0, 2);
		HorizontalPanel hpanel = new HorizontalPanel();
		table.setWidget(2, 0, hpanel);
		table.getCellFormatter().setAlignment(2, 0, HasHorizontalAlignment.ALIGN_RIGHT, HasVerticalAlignment.ALIGN_MIDDLE);
				
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
