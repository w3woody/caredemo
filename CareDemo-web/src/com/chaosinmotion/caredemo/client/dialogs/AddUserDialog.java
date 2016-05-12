/*	UserInfoDialog.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs;

import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.client.widgets.DialogWidget;
import com.chaosinmotion.caredemo.shared.Errors;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Add user. This prompts for the user's name and e-mail address. This is
 * the first step of onboarding; if the e-mail address is already in use,
 * we then carry to another screen 
 * @author woody
 *
 */
public class AddUserDialog extends DialogBox
{
	public interface Callback
	{
		void found(int userID);
		void created(int userID);
		void failure();
	}
	
	private Callback callback;
	private DialogWidget table;

	private TextBox name;
	private TextBox email;
	
	public AddUserDialog(Callback cb)
	{
		super(false,true);
		
		callback = cb;
		
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText("Add New User");
		
		VerticalPanel vpanel = new VerticalPanel();
		vpanel.setWidth("400px");

		table = new DialogWidget();
		table.setWidth("100%");
		
		vpanel.add(table);
		
		/*
		 * 	Format table
		 */
		
		name = table.addTextBox(0,"Name:");
		email = table.addTextBox(1,"EMail:");

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
		
		String label = "Find";
		BarButton saveButton = new BarButton(label);
		saveButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doDone();
			}
		});
		hpanel.add(saveButton);
		
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
	 * Look to see if someone matching this e-mail address exists. If they
	 * do, we appropriately prompt, then return success. If they don't exist,
	 * we appropriately prompt, then create the user and return success.
	 */
	private void doDone()
	{
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("manage/getBasicInfo"));
		req.put("email", new JSONString(email.getText()));
		
		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				JSONObject data = result.get("data").isObject();
				final int userID = (int)(data.get("userid").isNumber().doubleValue());
				new ConfirmationBox("User exists",
						"A user with that e-mail has been found. Do you "
						+ "wish to continue onboarding? If you do you will "
						+ "have an opportunity to update the user's permissions.",
						"Continue",new ConfirmationBox.Callback() {

							@Override
							public void canceled()
							{
							}

							@Override
							public void confirmed()
							{
								callback.found(userID);
								hide();
							}
				});
			}
			
			@Override
			public void error(int serverError)
			{
				if (serverError == Errors.NOSUCHUSER) {
					new ConfirmationBox("Create User",
							"The specified user does not exist in the system. Are"
							+ "you sure you wish to create them? If you do, this"
							+ "will add a new user, send the user an onboarding"
							+ "email message, and allow you to configure their"
							+ "profile.",
							"Create",new ConfirmationBox.Callback() {

								@Override
								public void canceled()
								{
								}

								@Override
								public void confirmed()
								{
									createNewUser();
								}
					});
				} else {
					// Assume network error that is prompted elsewhere
				}
			}
		});
	}
	
	/**
	 * Create a new user ready for onboarding
	 */
	
	private void createNewUser()
	{
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("manage/addUser"));
		req.put("email", new JSONString(email.getText()));
		req.put("name", new JSONString(name.getText()));

		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				JSONObject data = result.get("data").isObject();
				int userID = (int)(data.get("userid").isNumber().doubleValue());
				callback.created(userID);
				hide();
			}
			
			@Override
			public void error(int serverError)
			{
				new MessageBox("Error","Network error");
			}
		});
	}
}
