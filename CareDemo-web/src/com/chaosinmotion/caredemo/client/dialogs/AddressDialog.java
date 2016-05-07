/*	LoginDialog.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs;

import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.shared.Errors;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
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
public class AddressDialog extends DialogBox
{
	public interface Callback
	{
		void success();
		void failure();
	}
		
	private JSONObject addr;
	
	private Callback callback;
	private FlexTable table;
		
	private TextBox name;
	private TextBox addr1;
	private TextBox addr2;
	private TextBox city;
	private TextBox state;
	private TextBox postal;
	
	public AddressDialog(JSONObject a, Callback cb)
	{
		super(false,true);
		
		callback = cb;
		addr = a;
		
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText((a == null) ? "Add Address" : "Update Address");
		
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

		name = addTableRow(0,"Name (Home/Work/etc):");
		addr1 = addTableRow(1,"Address:");
		addr2 = addTableRow(2,"");
		city = addTableRow(3,"City:");
		state = addTableRow(4,"State:");
		postal = addTableRow(5,"Zip Code:");
		
		/*
		 * Populate if update
		 */
		
		if (a != null) {
			name.setText(a.get("name").isString().stringValue());
			addr1.setText(a.get("addr1").isString().stringValue());
			addr2.setText(a.get("addr2").isString().stringValue());
			city.setText(a.get("city").isString().stringValue());
			state.setText(a.get("state").isString().stringValue());
			postal.setText(a.get("postal").isString().stringValue());
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
	
	private TextBox addTableRow(int row, String label)
	{
		table.setText(row, 0, label);
		table.getCellFormatter().setStyleName(row, 0, "dialoglabel");
		
		TextBox text = new TextBox();
		text.setStyleName("dialogtextbox");
		table.setWidget(row, 1, text);
		
		return text;
	}
	

	/**
	 * Handle the login process
	 */
	private void doDone()
	{
		JSONObject req = new JSONObject();
		if (addr != null) {
			req.put("cmd", new JSONString("profile/updateAddress"));
			req.put("index", addr.get("index"));
		} else {
			req.put("cmd", new JSONString("profile/addAddress"));
		}
		// Run login process
		req.put("name", new JSONString(name.getText()));
		req.put("addr1", new JSONString(addr1.getText()));
		req.put("addr2", new JSONString(addr2.getText()));
		req.put("city", new JSONString(city.getText()));
		req.put("state", new JSONString(state.getText()));
		req.put("postal", new JSONString(postal.getText()));

		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				callback.success();
				hide();
			}
			
			@Override
			public void error(int serverError)
			{
				if (serverError == Errors.ACCESSVIOLATION) {
					new MessageBox("Error","Access violation.");
				} else {
					new MessageBox("Error","Network problem.");
				}
			}
		});
	}
}
