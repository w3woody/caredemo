/*	LoginDialog.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs;

import com.chaosinmotion.caredemo.client.network.Network;
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
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Handles the machinery of the login process. This exits with success or
 * failure depending on if the user logged in or not; if the user forgot his
 * password this will exit with failure. Use success to transition to the
 * logged in screen.
 * @author woody
 *
 */
public class LoginDialog extends DialogBox
{
	public interface Callback
	{
		void success();
		void failure();
	}
	
	private Callback callback;
	private DialogWidget table;
	private BarButton doneButton;
	
	private boolean forgotFlag;
	private TextBox username;
	private PasswordTextBox password;
	
	public LoginDialog(Callback cb)
	{
		super(false,true);
		
		callback = cb;
		
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText("Login");
		
		VerticalPanel vpanel = new VerticalPanel();
		vpanel.setWidth("400px");

		table = new DialogWidget();
		table.setWidth("100%");
		
		vpanel.add(table);
		
		/*
		 *  Add buttons
		 */

		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.setSpacing(4);
		vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		vpanel.add(hpanel);
		
		BarButton cancel = new BarButton("Cancel");
		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				callback.failure();
				hide();
			}
		});
		hpanel.add(cancel);
		
		doneButton = new BarButton("Login");
		doneButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doDone();
			}
		});
		hpanel.add(doneButton);
		
		setWidget(vpanel);
		
		/*
		 * Populate table for login
		 */
		
		populateLogin();
		
		/*
		 * Show the panel
		 */
		
		setPopupPositionAndShow(new PositionCallback() {
			@Override
			public void setPosition(int offsetWidth, int offsetHeight)
			{
				int width = Window.getClientWidth();
				int height = Window.getClientHeight();
				
				int xpos = (width - offsetWidth)/2;
				int ypos = (height - offsetHeight)/3;
				
				setPopupPosition(xpos, ypos);
			}
		});
	}
	
	private void insertForgetSelector(int row, final boolean select)
	{
		CheckBox cbox = new CheckBox("Forgot Password");
		cbox.setValue(select);
		cbox.setStyleName("dialogcheckbox");
		table.addRow(row,"",cbox);
		
		cbox.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				if (select) {
					populateLogin();
				} else {
					populateForgot();
				}
			}
		});
	}
	
	private void populateForgot()
	{
		forgotFlag = true;
		
		doneButton.setText("Forgot Password");
		
		table.clear();
		
		username = table.addTextBox(0, "E-Mail:");
		
		insertForgetSelector(1, true);
	}
	
	/**
	 * Clear and repopulate the flex table with login dialog box
	 */
	private void populateLogin()
	{
		forgotFlag = false;
		
		doneButton.setText("Login");
		
		table.clear();
		
		username = table.addTextBox(0, "Username:");
		password = table.addPasswordTextBox(1, "Password:");

		insertForgetSelector(2, false);
	}
	
	/**
	 * Handle the login process
	 */
	private void doDone()
	{
		if (forgotFlag) {
			// TODO: network call to the forgot password mechanism
			JSONObject req = new JSONObject();
			req.put("cmd", new JSONString("users/forgotPassword"));
			req.put("email", new JSONString(username.getText()));
			
			Network.get().request(req, new Network.ResultCallback() {
				@Override
				public void response(JSONObject result)
				{
					new MessageBox("E-mail sent","An e-mail has been sent with instructions on how to reset your password");
				}
								
				@Override
				public void error(int serverError)
				{
					new MessageBox("Error","Incorrect username/password pair. If you forgot your password, select \"Forgot Password\" to reset your password.");
				}
			});

		} else {
			// Run login process
			JSONObject req = new JSONObject();
			req.put("cmd", new JSONString("users/login"));
			req.put("username", new JSONString(username.getText()));
			String passwordHash = SHA256Hash.hash(password.getText() + Constants.SALT);
			req.put("password", new JSONString(passwordHash));
			
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
					
					callback.success();
					hide();
				}
								
				@Override
				public void error(int serverError)
				{
					new MessageBox("Error","Incorrect username/password pair. If you forgot your password, select \"Forgot Password\" to reset your password.");
				}
			});
		}
	}
}
