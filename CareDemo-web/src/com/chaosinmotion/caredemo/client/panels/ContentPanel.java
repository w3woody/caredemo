/*	ContentPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

/**
 * This represents a panel that contains a header, boundary bars, and content.
 * These are appended to the content panel area and represents blocks of data
 * inside of a panel
 * @author woody
 *
 */
public abstract class ContentPanel extends Composite
{
	private FlexTable panel;
	
	protected ContentPanel(String title)
	{
		panel = new FlexTable();
		
		panel.setBorderWidth(0);
		panel.setCellPadding(0);
		panel.setCellSpacing(0);
		
		panel.setStyleName("contentPanel");
		panel.setText(0, 0, title);
		panel.getCellFormatter().setStyleName(0, 0, "contentPanelTitle");
	}
	
	protected void setContent(Widget w)
	{
		w.setWidth("100%");
		panel.setWidget(1, 0, w);

		super.initWidget(panel);
	}
}
