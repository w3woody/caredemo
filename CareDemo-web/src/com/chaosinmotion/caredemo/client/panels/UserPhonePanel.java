/*	UserAddressPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.chaosinmotion.caredemo.client.dialogs.ConfirmationBox;
import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.dialogs.PhoneDialog;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.client.widgets.DialogWidget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author woody
 *
 */
public class UserPhonePanel extends ContentPanel
{
	private JSONArray array;
	private DialogWidget table;
	
	public interface Callback
	{
		void refresh();
	}
	
	private Callback callback;
	
	/**
	 * @param title
	 */
	public UserPhonePanel(Callback cb)
	{
		super("Phones");
		
		callback = cb;
		setContent(initWidget());
	}

	protected Widget initWidget()
	{
		table = new DialogWidget(true);
		return table;
	}
	
	private static String getValue(JSONObject addr, String field)
	{
		JSONString str = addr.get(field).isString();
		if (str == null) return null;
		String s = str.stringValue();
		if (s.length() == 0) return null;
		return s;
	}
	
	/**
	 * Initialize for editing.
	 * @param addrArray
	 */
	public void initialize(JSONArray addrArray)
	{
		table.clear();
		
		array = addrArray;
		int i,len = addrArray.size();
		for (i = 0; i < len; ++i) {
			JSONObject obj = addrArray.get(i).isObject();
			
			String name = getValue(obj,"name");
			if (name == null) name = "";
			
			String phone = getValue(obj,"phone");
			if (phone == null) phone = "";
			
			HorizontalPanel hp = table.addWideText(i, name, phone);

			final int index = i;
			BarButton update = new BarButton("Update");
			hp.add(update);
			update.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event)
				{
					updatePhone(index);
				}
			});
			BarButton remove = new BarButton("Delete");
			hp.add(remove);
			remove.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event)
				{
					removePhone(index);
				}
			});
		}

		HorizontalPanel hp = table.addWideText(i, "", "");
		
		BarButton update = new BarButton("Add");
		hp.add(update);
		update.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				addPhone();
			}
		});
	}
	
	/**
	 * Update the phone
	 * @param index
	 */
	private void updatePhone(int index)
	{
		JSONObject addr = array.get(index).isObject();
		new PhoneDialog(addr,new PhoneDialog.Callback() {
			@Override
			public void success()
			{
				callback.refresh();
			}
			
			@Override
			public void failure()
			{
			}
		});
	}
	
	/**
	 * Add an phone
	 */
	private void addPhone()
	{
		new PhoneDialog(null,new PhoneDialog.Callback() {
			@Override
			public void success()
			{
				callback.refresh();
			}
			
			@Override
			public void failure()
			{
			}
		});
	}
	
	/**
	 * Delete a phone
	 * @param index
	 */
	private void removePhone(final int index)
	{
		new ConfirmationBox("Are you sure?","This operation cannot be undone.","Remove",new ConfirmationBox.Callback() {
			@Override
			public void confirmed()
			{
				doRemovePhone(index);
			}
			
			@Override
			public void canceled()
			{
			}
		});
	}
	
	/**
	 * Remove the phone row
	 * @param index
	 */
	private void doRemovePhone(int index)
	{
		JSONObject addr = array.get(index).isObject();
		JSONObject obj = new JSONObject();
		obj.put("cmd", new JSONString("profile/removePhone"));
		obj.put("index", addr.get("index"));
		Network.get().request(obj,new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				callback.refresh();
			}
			
			@Override
			public void error(int serverError)
			{
				new MessageBox("Error","Network error");
			}
		});
	}
}
