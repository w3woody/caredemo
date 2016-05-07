/*	UserAddressPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.chaosinmotion.caredemo.client.dialogs.AddressDialog;
import com.chaosinmotion.caredemo.client.dialogs.ConfirmationBox;
import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author woody
 *
 */
public class UserAddressPanel extends ContentPanel
{
	private JSONArray array;
	private FlexTable table;
	
	public interface Callback
	{
		void refresh();
	}
	
	private Callback callback;
	
	/**
	 * @param title
	 */
	public UserAddressPanel(Callback cb)
	{
		super("Address");
		
		callback = cb;
	}

	/* (non-Javadoc)
	 * @see com.chaosinmotion.caredemo.client.panels.ContentPanel#initWidget()
	 */
	@Override
	protected Widget initWidget()
	{
		table = new FlexTable();
		table.setWidth("100%");
		
		table.setCellPadding(8);
		table.setCellSpacing(0);
		table.setBorderWidth(0);
		
		table.getColumnFormatter().setWidth(0, "120px");
		table.getColumnFormatter().setWidth(2, "90px");
		
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
	 * Return multi-line list
	 * @param addr
	 * @return
	 */
	private static VerticalPanel formatAddress(JSONObject addr)
	{
		VerticalPanel vp = new VerticalPanel();
		
		String s = getValue(addr,"addr1");
		if (s != null) {
			Label l = new Label(s);
			l.setStyleName("dialoglabel");
			vp.add(l);
		}
		
		s = getValue(addr,"addr2");
		if (s != null) {
			Label l = new Label(s);
			l.setStyleName("dialoglabel");
			vp.add(l);
		}
		
		StringBuffer buffer = new StringBuffer();
		s = getValue(addr,"city");
		if (s != null) {
			buffer.append(s);
		}
		s = getValue(addr,"state");
		if (s != null) {
			if (buffer.length() > 0) buffer.append(' ');
			buffer.append(s);
		}
		s = getValue(addr,"postal");
		if (s != null) {
			if (buffer.length() > 0) buffer.append(' ');
			buffer.append(s);
		}
		if (buffer.length() > 0) {
			Label l = new Label(buffer.toString());
			l.setStyleName("dialoglabel");
			vp.add(l);
		}
		
		return vp;
	}
	
	/**
	 * Initialize for editing.
	 * @param addrArray
	 */
	public void initialize(JSONArray addrArray)
	{
		table.removeAllRows();
		
		array = addrArray;
		int i,len = addrArray.size();
		for (i = 0; i < len; ++i) {
			JSONObject obj = addrArray.get(i).isObject();
			
			String str = getValue(obj,"name");
			if (str != null) {
				table.setText(i, 0, str);
				table.getCellFormatter().setStyleName(i, 0, "dialoglabel");
			}
			
			VerticalPanel vp = formatAddress(obj);
			table.setWidget(i, 1, vp);
			
			HorizontalPanel hp = new HorizontalPanel();
			table.getCellFormatter().setHorizontalAlignment(i, 2, HasHorizontalAlignment.ALIGN_RIGHT);
			table.setWidget(i, 2, hp);
			
			final int addrIndex = i;
			BarButton update = new BarButton("Update");
			hp.add(update);
			update.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event)
				{
					updateAddress(addrIndex);
				}
			});
			BarButton remove = new BarButton("Delete");
			hp.add(remove);
			remove.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event)
				{
					removeAddress(addrIndex);
				}
			});
		}

		HorizontalPanel hp = new HorizontalPanel();
		table.getCellFormatter().setHorizontalAlignment(i, 2, HasHorizontalAlignment.ALIGN_RIGHT);
		table.setWidget(i, 2, hp);
		
		BarButton update = new BarButton("Add");
		hp.add(update);
		update.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				addAddress();
			}
		});
	}
	
	/**
	 * Update the address
	 * @param index
	 */
	private void updateAddress(int index)
	{
		JSONObject addr = array.get(index).isObject();
		new AddressDialog(addr,new AddressDialog.Callback() {
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
	 * Add an address
	 */
	private void addAddress()
	{
		new AddressDialog(null,new AddressDialog.Callback() {
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
	 * Delete an address
	 * @param index
	 */
	private void removeAddress(final int index)
	{
		new ConfirmationBox("Are you sure?","This operation cannot be undone.","Remove",new ConfirmationBox.Callback() {
			@Override
			public void confirmed()
			{
				doRemoveAddress(index);
			}
			
			@Override
			public void canceled()
			{
			}
		});
	}
	
	/**
	 * Remove the address row
	 * @param index
	 */
	private void doRemoveAddress(int index)
	{
		JSONObject addr = array.get(index).isObject();
		JSONObject obj = new JSONObject();
		obj.put("cmd", new JSONString("profile/removeAddress"));
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
