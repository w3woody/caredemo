/*	LoginDialog.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs.edit;

import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.client.widgets.DialogWidget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
public class EditPhoneDialog extends DialogBox
{
	public interface Callback
	{
		void success(Phone phone);
		void failure();
	}
	
	private Phone phoneData;
	
	private Callback callback;
	private DialogWidget table;
		
	private TextBox name;
	private TextBox phone;
	
	public EditPhoneDialog(Phone p, Callback cb)
	{
		super(false,true);
		
		callback = cb;
		phoneData = p;
		
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText((p == null) ? "Add Phone" : "Update Phone");
		
		VerticalPanel vpanel = new VerticalPanel();
		vpanel.setWidth("400px");

		table = new DialogWidget();
		table.setWidth("100%");
		
		vpanel.add(table);
		
		/*
		 * 	Format table
		 */
		
		name = table.addTextBox(0,"Name (Home/Cell/etc):");
		phone = table.addTextBox(1,"Phone:");
		
		/*
		 * Populate if update
		 */
		
		if (p != null) {
			name.setText(p.name);
			phone.setText(p.phone);
		}

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
		
		String label = (phoneData == null) ? "Add" : "Update";
		BarButton saveButton = new BarButton(label);
		saveButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doDone();
				hide();
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
	 * Handle the login process
	 */
	private void doDone()
	{
		if (phoneData == null) {
			phoneData = new Phone();
			phoneData.phoneID = 0;
		} else {
			phoneData.edit = true;
		}
		phoneData.name = name.getText();
		phoneData.phone = phone.getText();
		
		callback.success(phoneData);
	}
}
