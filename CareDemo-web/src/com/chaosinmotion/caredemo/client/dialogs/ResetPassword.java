/*	LoginDialog.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs;

import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
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
import com.google.gwt.user.client.ui.FlexTable;
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
public class ResetPassword extends DialogBox
{
	public interface Callback
	{
		void success();
		void failure();
	}
	
	private String token;
	
	private Callback callback;
	private FlexTable table;
	private BarButton doneButton;
	
	private PasswordTextBox password1;
	private PasswordTextBox password2;
	
	public ResetPassword(String t, Callback cb)
	{
		super(false,true);
		
		callback = cb;
		token = t;
		
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText("Login");
		
		VerticalPanel vpanel = new VerticalPanel();
		vpanel.setWidth("400px");

		table = new FlexTable();
		table.setWidth("100%");
		
		vpanel.add(table);
		
		/*
		 * 	Format table
		 */
		
		table.setCellPadding(8);
		table.setCellPadding(0);
		table.setBorderWidth(0);
		table.getColumnFormatter().setWidth(0, "120px");

		table.setText(0, 0, "Password:");
		table.getCellFormatter().setStyleName(0, 0, "dialoglabel");
		
		password1 = new PasswordTextBox();
		password1.setStyleName("dialogtextbox");
		table.setWidget(0, 1, password1);

		table.setText(1, 0, "Retype Password:");
		table.getCellFormatter().setStyleName(1, 0, "dialoglabel");
		
		password2 = new PasswordTextBox();
		password2.setStyleName("dialogtextbox");
		table.setWidget(1, 1, password2);

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
	

	/**
	 * Handle the login process
	 */
	private void doDone()
	{
		String p1 = password1.getText();
		String p2 = password2.getText();
		if (!p1.equals(p2)) {
			new MessageBox("Error","Passwords do not match");
			return;
		}
		
		String penc = SHA256Hash.hash(p1 + Constants.SALT);
		
		// Run login process
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("users/forgotPassword"));
		req.put("token", new JSONString(token));
		req.put("password", new JSONString(penc));

		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				if (result.get("success").isBoolean().booleanValue()) {
					/*
					 * Stash away the login information in persistant store
					 * and return success. If we dont' have persistant store
					 * we wind up making a round trip to the server as we
					 * switch pages.
					 */
					
					Storage storage = Storage.getSessionStorageIfSupported();
					if (storage != null) {
						storage.setItem("user", result.toString());
					}
					
					callback.success();
					hide();
				} else {
					new MessageBox("Error","Incorrect username/password pair. If you forgot your password, select \"Forgot Password\" to reset your password.");
				}
			}
			
			@Override
			public void error(int serverError)
			{
				new MessageBox("Error","An unknown network problem occurred.");
			}
		});
	}
}
