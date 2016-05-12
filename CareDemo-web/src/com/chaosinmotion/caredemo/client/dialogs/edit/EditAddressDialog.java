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
public class EditAddressDialog extends DialogBox
{
	public interface Callback
	{
		void success(Address addr);
		void failure();
	}
		
	private Address addrData;
	
	private Callback callback;
	private DialogWidget table;
		
	private TextBox name;
	private TextBox addr1;
	private TextBox addr2;
	private TextBox city;
	private TextBox state;
	private TextBox postal;
	
	public EditAddressDialog(Address a, Callback cb)
	{
		super(false,true);
		
		callback = cb;
		addrData = a;
		
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText((a == null) ? "Add Address" : "Update Address");
		
		VerticalPanel vpanel = new VerticalPanel();
		vpanel.setWidth("400px");

		table = new DialogWidget();
		table.setWidth("100%");
		
		vpanel.add(table);
		
		/*
		 * 	Format table
		 */
		
		name = table.addTextBox(0,"Name (Home/Work/etc):");
		addr1 = table.addTextBox(1,"Address:");
		addr2 = table.addTextBox(2,"");
		city = table.addTextBox(3,"City:");
		state = table.addTextBox(4,"State:");
		postal = table.addTextBox(5,"Zip Code:");
		
		/*
		 * Populate if update
		 */
		
		if (a != null) {
			name.setText(a.name);
			addr1.setText(a.addr1);
			addr2.setText(a.addr2);
			city.setText(a.city);
			state.setText(a.state);
			postal.setText(a.postal);
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
		
		String label = (a == null) ? "Add" : "Update";
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
		if (addrData == null) {
			addrData = new Address();
			addrData.addrID = 0;
		} else {
			addrData.edit = true;
		}
		addrData.name = name.getText();
		addrData.addr1 = addr1.getText();
		addrData.addr2 = addr2.getText();
		addrData.city = city.getText();
		addrData.state = state.getText();
		addrData.postal = postal.getText();
		
		callback.success(addrData);
	}
}
