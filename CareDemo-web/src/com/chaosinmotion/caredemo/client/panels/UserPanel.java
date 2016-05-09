/*	UserPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.client.widgets.PromptTextBox;
import com.chaosinmotion.caredemo.shared.Errors;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author woody
 *
 */
public class UserPanel extends ContentPanel
{
	int page;
	private FlexTable table;
	private PromptTextBox textBox;

	public UserPanel(String p)
	{
		super("Users");
		
		if (p.equals("patients")) {
			page = 2;
		} else {
			page = 1;
		}
	}

	/* (non-Javadoc)
	 * @see com.chaosinmotion.caredemo.client.panels.ContentPanel#initWidget()
	 */
	@Override
	protected Widget initWidget()
	{
		table = new FlexTable();
		table.getColumnFormatter().setWidth(0, "250px");
		table.getColumnFormatter().setWidth(2, "50px");
		
		table.setBorderWidth(0);
		table.setCellPadding(8);
		table.setCellSpacing(0);
		
		table.getFlexCellFormatter().setColSpan(0, 0, 2);
		textBox = new PromptTextBox("Search");
		textBox.setStyleName("dialogtextbox");
		table.setWidget(0, 0, textBox);
		table.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
		
		textBox.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event)
			{
				doSearch();
			}
		});
		
		BarButton btn = new BarButton("Onboard");
		table.setWidget(0, 1, btn);
		btn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				// TODO: Run onboarding for this
			}
		});

		return table;
	}
	
	private void reloadFromArray(JSONArray array)
	{
		int i,len = table.getRowCount();
		for (i = len-1; i >= 1; --i) table.removeRow(i);
		
		len = array.size();
		for (i = 0; i < len; ++i) {
			JSONObject item = array.get(i).isObject();
			String name = item.get("name").isString().stringValue();
			String email = item.get("email").isString().stringValue();
			
			table.setText(i+1, 0, name);
			table.getCellFormatter().setStyleName(i+1, 0, "dialoglabel");
			table.setText(i+1, 1, email);
			table.getCellFormatter().setStyleName(i+1, 1, "dialoglabel");
			
			final int userID = (int)(item.get("userid").isNumber().doubleValue());
			HorizontalPanel hp = new HorizontalPanel();
			table.setWidget(i+1, 2, hp);
			
			BarButton btn = new BarButton("Update");
			hp.add(btn);
			btn.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event)
				{
					doUpdateUser(userID);
				}
			});
			btn = new BarButton("Delete");
			hp.add(btn);
			btn.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event)
				{
					doDeleteUser(userID);
				}
			});
			
		}
	}
	
	private void doUpdateUser(int userID)
	{
		
	}
	
	private void doDeleteUser(int userID)
	{
		
	}
	
	/**
	 * Do search
	 */
	private void doSearch()
	{
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("manage/search"));
		req.put("search", new JSONString(textBox.getText()));
		req.put("type", new JSONNumber(page));
		Network.get().request(req, new Network.ResultCallback() {
			
			@Override
			public void response(JSONObject result)
			{
				JSONObject obj = result.get("data").isObject();
				JSONArray array = obj.get("results").isArray();
				reloadFromArray(array);
			}
			
			@Override
			public void error(int serverError)
			{
				if (serverError == Errors.ACCESSVIOLATION) {
					Window.Location.assign("index.html");
				} else {
					new MessageBox("Error","An error occured");
				}
			}
		});
	}
}
