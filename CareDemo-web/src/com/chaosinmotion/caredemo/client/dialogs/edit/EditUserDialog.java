/*	EditUserDialog.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs.edit;

import java.util.ArrayList;
import java.util.HashSet;
import com.chaosinmotion.caredemo.client.dialogs.ConfirmationBox;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.client.widgets.DialogWidget;
import com.chaosinmotion.caredemo.shared.ACE;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * @author woody
 *
 */
public class EditUserDialog extends DialogBox
{
	public interface Callback
	{
		void success();
		void failure();
	}
	
	private Callback callback;
	private DialogWidget table;
	
	private int userID;
	private boolean adminFlag;
	private TextBox name;
	private TextBox email;
	private CheckBox adminCheckBox;
	private CheckBox hcpCheckBox;
	private CheckBox patientCheckBox;
	
	private ArrayList<Address> addressList;
	private ArrayList<Phone> phoneList;
	private HashSet<Integer> addressDelete;
	private HashSet<Integer> phoneDelete;
	
	/*
	 * 	Internal storage
	 */
		
	public EditUserDialog(int uid, boolean a, Callback cb)
	{
		super(false,true);
		
		callback = cb;
		userID = uid;
		adminFlag = a;
		
		phoneList = new ArrayList<Phone>();
		addressList = new ArrayList<Address>();
		addressDelete = new HashSet<Integer>();
		phoneDelete = new HashSet<Integer>();
		
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText("Edit User");
		
		VerticalPanel vpanel = new VerticalPanel();
		vpanel.setWidth("500px");

		table = new DialogWidget(true);
		table.setWidth("100%");
		
		vpanel.add(table);
		
		/*
		 * Add name, email, admin flag
		 */
		
		name = table.addTextBox(0, "Name:");
		email = table.addTextBox(1, "EMail:");
		HorizontalPanel flags = new HorizontalPanel();
		if (adminFlag) {
			adminCheckBox = new CheckBox("Administrator");
			adminCheckBox.setStyleName("dialogcheckbox");
			flags.add(adminCheckBox);
			
			hcpCheckBox = new CheckBox("Health Care Professional");
			hcpCheckBox.setStyleName("dialogcheckbox");
			flags.add(hcpCheckBox);
		} else {
			patientCheckBox = new CheckBox("Patient");
			patientCheckBox.setStyleName("dialogcheckbox");
			flags.add(patientCheckBox);
		}
		
		table.addRow(2, "Privileges", flags);

		table.addSeparator(3);
		table.addText(4, "Addresses");
		HorizontalPanel hp = table.addButtonPanel(5);
		BarButton addAddress = new BarButton("Add");
		addAddress.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doAddAddress();
			}
		});
		hp.add(addAddress);

		table.addSeparator(6);
		table.addText(7, "Phones");
		hp = table.addButtonPanel(8);
		BarButton addPhone = new BarButton("Add");
		addPhone.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doAddPhone();
			}
		});
		hp.add(addPhone);
		table.addSeparator(9);

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
		
		BarButton save = new BarButton("Save");
		save.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doSave();
			}
		});
		hpanel.add(save);
		
		setWidget(vpanel);
		
		/*
		 * Show the panel
		 */
		
		setPopupPositionAndShow(new PositionCallback() {
			@Override
			public void setPosition(int offsetWidth, int offsetHeight)
			{
				int width = Window.getClientWidth();
				
				int xpos = (width - offsetWidth)/2;
				int ypos = 25;
				
				setPopupPosition(xpos, ypos);
			}
		});
		
		reloadData();
	}
	
	/**
	 * Reload the contents
	 */
	private void reloadData()
	{
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("profile/getBasicInfo"));
		req.put("userid", new JSONNumber(userID));
		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				JSONObject data = result.get("data").isObject();
				String n = data.get("name").isString().stringValue();
				String e = data.get("email").isString().stringValue();
				
				name.setText(n);
				email.setText(e);
				JSONArray a = data.get("acl").isArray();
				HashSet<Integer> h = new HashSet<Integer>();
				int i,len = a.size();
				for (i = 0; i < len; ++i) {
					h.add((int)(a.get(i).isNumber().doubleValue()));
				}
				
				if (adminFlag) {
					adminCheckBox.setValue(h.contains(ACE.Administrator));
					hcpCheckBox.setValue(h.contains(ACE.HealthCareProvider));
				} else {
					patientCheckBox.setValue(h.contains(ACE.Patient));
				}
				
				reloadContents();
			}
			
			@Override
			public void error(int serverError)
			{
			}
		});
	}

	/**
	 * Return multi-line list
	 * @param addr
	 * @return
	 */
	private static VerticalPanel formatAddress(Address addr)
	{
		VerticalPanel vp = new VerticalPanel();
		
		String s = addr.addr1;
		if ((s != null) && (s.length() > 0)) {
			Label l = new Label(s);
			l.setStyleName("dialoglabel");
			vp.add(l);
		}
		
		s = addr.addr2;
		if ((s != null) && (s.length() > 0)) {
			Label l = new Label(s);
			l.setStyleName("dialoglabel");
			vp.add(l);
		}
		
		StringBuffer buffer = new StringBuffer();
		s = addr.city;
		if ((s != null) && (s.length() > 0)) {
			buffer.append(s);
		}
		s = addr.state;
		if ((s != null) && (s.length() > 0)) {
			if (buffer.length() > 0) buffer.append(' ');
			buffer.append(s);
		}
		s = addr.postal;
		if ((s != null) && (s.length() > 0)) {
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
	
	private void setAddressAtRow(final int row, final Address addr)
	{
		HorizontalPanel hp = table.addWideWidget(row, addr.name, formatAddress(addr));
		
		BarButton btn;
		btn = new BarButton("Update");
		btn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				new EditAddressDialog(addr,new EditAddressDialog.Callback() {
					@Override
					public void success(Address addr)
					{
						setAddressAtRow(row,addr);
					}
					
					@Override
					public void failure()
					{
					}
				});
			}
		});
		hp.add(btn);
		
		btn = new BarButton("Delete");
		hp.add(btn);
		btn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				new ConfirmationBox("Are you sure?","Are you sure you wish to delete?","Delete",new ConfirmationBox.Callback() {
					@Override
					public void confirmed()
					{
						int index = row - 5;
						table.removeRow(row);
						addressList.remove(index);
						if (addr.addrID != 0) {
							addressDelete.add(addr.addrID);
						}
					}
					
					@Override
					public void canceled()
					{
					}
				});
			}
		});
	}
	
	private void setPhoneAtRow(final int row, final Phone phone)
	{
		HorizontalPanel hp = table.addWideText(row, phone.name, phone.phone);
		
		BarButton btn;
		btn = new BarButton("Update");
		btn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				new EditPhoneDialog(phone,new EditPhoneDialog.Callback() {
					@Override
					public void success(Phone phone)
					{
						setPhoneAtRow(row,phone);
					}
					
					@Override
					public void failure()
					{
					}
				});
			}
		});
		hp.add(btn);
		btn = new BarButton("Delete");
		hp.add(btn);
		btn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				new ConfirmationBox("Are you sure?","Are you sure you wish to delete?","Delete",new ConfirmationBox.Callback() {
					@Override
					public void confirmed()
					{
						int index = row - 8 - addressList.size();
						table.removeRow(row);
						phoneList.remove(index);
						if (phone.phoneID != 0) {
							phoneDelete.add(phone.phoneID);
						}
					}
					
					@Override
					public void canceled()
					{
					}
				});
			}
		});
	}

	private void reloadContents()
	{
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("profile/getFullProfile"));
		req.put("userid", new JSONNumber(userID));
		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				JSONObject data = result.get("data").isObject();
				
				JSONArray addrArray = data.get("addresses").isArray();
				JSONArray phoneArray = data.get("phones").isArray();
				
				/*
				 * Insert
				 */
				
				int i,len = addrArray.size();
				for (i = 0; i < len; ++i) {
					Address addr = new Address(addrArray.get(i).isObject());
					addressList.add(addr);
					
					int row = 5 + i;
					table.insertRow(row);
					setAddressAtRow(row,addr);
				}
				int plen = phoneArray.size();
				for (i = 0; i < plen; ++i) {
					Phone phone = new Phone(phoneArray.get(i).isObject());
					phoneList.add(phone);
					
					int row = 8 + len + i;
					table.insertRow(row);
					setPhoneAtRow(row,phone);
				}
			}
			
			@Override
			public void error(int serverError)
			{
			}
		});
	}
	
	private void doAddAddress()
	{
		new EditAddressDialog(null,new EditAddressDialog.Callback() {
			@Override
			public void success(Address addr)
			{
				int row = 5 + addressList.size();
				addressList.add(addr);
				
				table.insertRow(row);
				setAddressAtRow(row,addr);
			}
			
			@Override
			public void failure()
			{
			}
		});
	}
	
	private void doAddPhone()
	{
		new EditPhoneDialog(null,new EditPhoneDialog.Callback() {
			@Override
			public void success(Phone phone)
			{
				int row = 8 + addressList.size() + phoneList.size();
				phoneList.add(phone);
				
				table.insertRow(row);
				setPhoneAtRow(row,phone);
			}
			
			@Override
			public void failure()
			{
			}
		});
	}
	
	public void doSave()
	{
		/*
		 * Construct save information
		 */
		
		JSONObject req = new JSONObject();
		req.put("cmd", new JSONString("manage/updateUserData"));
		
		/*
		 *	Construct the basic stuff
		 */
		
		req.put("userid", new JSONNumber(userID));
		req.put("name", new JSONString(name.getText()));
		req.put("email", new JSONString(email.getText()));
		req.put("admin", JSONBoolean.getInstance(adminFlag));
		
		int index = 0;
		JSONArray array = new JSONArray();
		if (adminFlag) {
			if (adminCheckBox.getValue()) {
				array.set(index++, new JSONNumber(ACE.Administrator));
			}
			if (hcpCheckBox.getValue()) {
				array.set(index++, new JSONNumber(ACE.HealthCareProvider));
			}
		} else {
			if (patientCheckBox.getValue()) {
				array.set(index++, new JSONNumber(ACE.Patient));
			}
		}
		req.put("ace", array);
		
		/*
		 * Put in the add/edit/update list
		 */
		
		array = new JSONArray();
		index = 0;
		
		for (Integer ix: addressDelete) {
			JSONObject item = new JSONObject();
			item.put("item", new JSONString("address"));
			item.put("cmd", new JSONString("delete"));
			item.put("index", new JSONNumber(ix.intValue()));
			array.set(index++, item);
		}
		for (Address addr: addressList) {
			if ((addr.addrID != 0) && !addr.edit) continue;
			
			JSONObject item = new JSONObject();
			item.put("item", new JSONString("address"));
			if (addr.addrID == 0) {
				item.put("cmd", new JSONString("add"));
			} else {
				item.put("cmd", new JSONString("update"));
				item.put("index", new JSONNumber(addr.addrID));
			}
			item.put("name", new JSONString(addr.name));
			item.put("addr1", new JSONString(addr.addr1));
			item.put("addr2", new JSONString(addr.addr2));
			item.put("city", new JSONString(addr.city));
			item.put("state", new JSONString(addr.state));
			item.put("postal", new JSONString(addr.postal));
			array.set(index++, item);
		}
		
		for (Integer ix: phoneDelete) {
			JSONObject item = new JSONObject();
			item.put("item", new JSONString("phone"));
			item.put("cmd", new JSONString("delete"));
			item.put("index", new JSONNumber(ix.intValue()));
			array.set(index++, item);
		}
		for (Phone phone: phoneList) {
			if ((phone.phoneID != 0) && !phone.edit) continue;
			
			JSONObject item = new JSONObject();
			item.put("item", new JSONString("phone"));
			if (phone.phoneID == 0) {
				item.put("cmd", new JSONString("add"));
			} else {
				item.put("cmd", new JSONString("update"));
				item.put("index", new JSONNumber(phone.phoneID));
			}
			item.put("name", new JSONString(phone.name));
			item.put("phone", new JSONString(phone.phone));
			array.set(index++, item);
		}
		
		req.put("contents", array);
		
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
			}
		});
	}
}
