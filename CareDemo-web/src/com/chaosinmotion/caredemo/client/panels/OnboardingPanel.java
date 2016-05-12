/*	OnboardingPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.util.PasswordComplexity;
import com.chaosinmotion.caredemo.client.util.UserInfo;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.client.widgets.DialogWidget;
import com.chaosinmotion.caredemo.shared.Constants;
import com.chaosinmotion.caredemo.shared.SHA256Hash;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

/**
 * @author woody
 *
 */
public class OnboardingPanel extends Composite
{
	private String token;
	private DialogWidget widget;
	
	private TextBox username;
	private PasswordTextBox password1;
	private PasswordTextBox password2;

	/**
	 * @param token
	 */
	public OnboardingPanel(String t)
	{
		token = t;
		if ((token == null) || (token.length() == 0)) {
			Window.Location.replace("index.html");
			initWidget(new Label("error"));
			return;
		}
		
		widget = new DialogWidget();
		initWidget(widget);
		
		username = widget.addTextBox(0, "Username:");
		password1 = widget.addPasswordTextBox(1, "Password:");
		password2 = widget.addPasswordTextBox(2, "Repeat Password:");
		
		HorizontalPanel hp = widget.addButtonPanel(3);
		BarButton btn = new BarButton("Sign Up");
		hp.add(btn);
		
		btn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				String pwd1 = password1.getText();
				String pwd2 = password2.getText();
				
				if (!pwd1.equals(pwd2)) {
					new MessageBox("Password fields don't match","The "
							+ "password fields must match.");
					return;
				}
				
				if (!PasswordComplexity.isComplex(pwd1)) {
					new MessageBox("Insufficient password","Your password "
							+ "must be at least 8 characters long, and contain "
							+ "an uppercase letter, lowercase letter, number "
							+ "and punctuation mark.");
					return;
				}
				
				String pwdEnc = SHA256Hash.hash(pwd1 + Constants.SALT);
				
				JSONObject req = new JSONObject();
				req.put("cmd", new JSONString("onboard/onboard"));
				req.put("username", new JSONString(username.getText()));
				req.put("password", new JSONString(pwdEnc));
				req.put("token", new JSONString(token));
				
				Network.get().request(req, new Network.ResultCallback() {
					@Override
					public void response(JSONObject result)
					{
						/*
						 * Stash away the login information in persistant store
						 * and return success. If we dont' have persistant store
						 * we wind up making a round trip to the server as we
						 * switch pages.
						 */
						
						JSONObject userInfo = result.get("data").isObject();
						Storage storage = Storage.getSessionStorageIfSupported();
						if (storage != null) {
							storage.setItem("user", userInfo.toString());
						}
						UserInfo.get().setUserInfo(userInfo);
						
						Window.Location.assign("home.html");
					}
					
					@Override
					public void error(int serverError)
					{
						new MessageBox("Duplicate username?","Unable to create "
								+ "account; could the username be a duplicate?");
					}
				});
			}
		});
	}
	
}
