/*	PatientPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author woody
 *
 */
public class HCPCommandPanel extends ContentPanel
{
	private VerticalPanel vpanel;
	
	public HCPCommandPanel()
	{
		super("Patient Care");
	}

	@Override
	protected Widget initWidget()
	{
		vpanel = new VerticalPanel();
		vpanel.setSpacing(8);
		
		BarButton btn;
		
		/*
		 * Functions an administrator can perform:
		 * 
		 * An administrator can manage the HCPs in the system, but cannot
		 * manage patients. An administrator can also modify permissions
		 * if necessary.
		 */
		
		btn = new BarButton("Manage Patients");
		vpanel.add(btn);
		btn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				Window.Location.assign("users.html?p=patients");
			}
		});
		
		btn = new BarButton("Onboard New Patient");
		vpanel.add(btn);
		
		btn = new BarButton("Manage Patient Care Information");
		vpanel.add(btn);
		
		return vpanel;
	}
}
