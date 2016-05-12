/*	LoginDialog.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs;

import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.client.widgets.DialogWidget;
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
 * Handles the machinery of the login process. This exits with success or
 * failure depending on if the user logged in or not; if the user forgot his
 * password this will exit with failure. Use success to transition to the
 * logged in screen.
 * @author woody
 *
 */
public class AddMobileDeviceDialog extends DialogBox
{
	public interface Callback
	{
		void success();
		void failure();
	}
		
	private Callback callback;
	private DialogWidget table;
	private BarButton doneButton;
	
	private TextBox code;
	
	public AddMobileDeviceDialog(Callback cb)
	{
		super(false,true);
		
		callback = cb;
		
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText("Add Mobile Device");
		
		VerticalPanel vpanel = new VerticalPanel();
		vpanel.setWidth("400px");

		table = new DialogWidget();
		table.setWidth("100%");
		
		vpanel.add(table);
		
		/*
		 * 	Format table
		 */

		table.addText(0, "Make sure the CareDemo application is running on " + 
				"your phone. Please enter the 8 character registration code shown there.");

		code = table.addTextBox(1, "Code:");

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
		
		doneButton = new BarButton("Add");
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
		// Run login process
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("mobile/connect"));
		req.put("key", new JSONString(code.getText()));

		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				new MessageBox("Device Registered","This may take a few " + 
						"moments to complete registration.",new MessageBox.Callback() {
					@Override
					public void finished()
					{
						callback.success();
						hide();
					}
				});
			}
			
			@Override
			public void error(int serverError)
			{
				new MessageBox("Error","Incorrect registration code.");
			}
		});
	}
}
