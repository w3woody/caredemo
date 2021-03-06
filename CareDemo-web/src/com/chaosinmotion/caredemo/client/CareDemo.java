
package com.chaosinmotion.caredemo.client;

import com.chaosinmotion.caredemo.client.dialogs.LoginDialog;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class CareDemo implements EntryPoint
{
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad()
	{
		/*
		 * Hook the existing HTML elements
		 */
		
		RootPanel toolPanel = RootPanel.get("toolspanel");
		
		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.setSpacing(0);
		toolPanel.add(hpanel);
		
		BarButton login = new BarButton("Login");
		hpanel.add(login);
		
		login.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				new LoginDialog(new LoginDialog.Callback() {
					
					@Override
					public void success()
					{
						Window.Location.assign("home.html");
					}
					
					@Override
					public void failure()
					{
						// Ignore
					}
				});
			}
		});
	}
}
