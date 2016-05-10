/*	EditUserDialog.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.dialogs;

import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
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
	private FlexTable table;
		
	public EditUserDialog(int userID, Callback cb)
	{
		super(false,true);
		
		callback = cb;
		
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText("Edit User");
		
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
		
		// TODO: Add fields

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
				int height = Window.getClientHeight();
				
				int xpos = (width - offsetWidth)/2;
				int ypos = (height - offsetHeight)/3;
				
				setPopupPosition(xpos, ypos);
			}
		});
	}
	
	public void doSave()
	{
		// TODO: save
	}
}
