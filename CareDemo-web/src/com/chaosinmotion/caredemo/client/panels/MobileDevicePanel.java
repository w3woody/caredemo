/*	MobileDevicePanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.chaosinmotion.caredemo.client.dialogs.AddMobileDeviceDialog;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author woody
 *
 */
public class MobileDevicePanel extends ContentPanel
{
	private FlexTable table;
	
	public MobileDevicePanel()
	{
		super("Mobile Devices");
		setContent(initWidget());
	}

	protected Widget initWidget()
	{
		table = new FlexTable();
		table.getColumnFormatter().setWidth(1, "50px");
		
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("mobile/devices"));
		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				JSONObject data = result.get("data").isObject();
				JSONArray array = data.get("devices").isArray();
				
				int i,len = array.size();
				int row = 0;
				for (i = 0; i < len; ++i) {
					JSONObject device = array.get(i).isObject();
					table.setText(row, 0, device.get("description").isString().stringValue());
					table.getCellFormatter().setStyleName(row, 0, "dialogtext");
					
					BarButton btn = new BarButton("Delete");
					table.setWidget(row, 1, btn);
					
					final int deviceID = (int)(device.get("deviceid").isNumber().doubleValue());
					btn.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event)
						{
							deleteDevice(deviceID);
						}
					});
					
					++row;
				}
				
				BarButton add = new BarButton("Add New Mobile Device");
				table.setWidget(row,0,add);
				
				add.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event)
					{
						addDevice();
					}
				});
			}
			
			@Override
			public void error(int serverError)
			{
				table.setText(0, 0, "Network error");
				table.getCellFormatter().setStyleName(0, 0, "contentPanelErrorText");
			}
		});
		
		return table;
	}
	
	private void deleteDevice(int deviceID)
	{
		// TODO: Delete
	}
	
	private void addDevice()
	{
		new AddMobileDeviceDialog(new AddMobileDeviceDialog.Callback() {
			@Override
			public void success()
			{
			}
			
			@Override
			public void failure()
			{
			}
		});
	}
}
