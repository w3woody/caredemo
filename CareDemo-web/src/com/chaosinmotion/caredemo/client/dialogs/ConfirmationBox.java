package com.chaosinmotion.caredemo.client.dialogs;

import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ConfirmationBox extends DialogBox
{
	public interface Callback
	{
		void canceled();
		void confirmed();
	}
		
	public ConfirmationBox(String title, String message, String confirmLabel, final Callback callback)
	{
		super(false,true);
				
		setStyleName("messageBox");
		setGlassEnabled(true);
		setText(title);
		
		VerticalPanel vpanel = new VerticalPanel();
		vpanel.setWidth("400px");
		
		Label l = new Label(message);
		vpanel.add(l);
		
		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.setSpacing(4);
		vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		vpanel.add(hpanel);
		
		BarButton cancel = new BarButton("Cancel");
		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				callback.canceled();
				hide();
			}
		});
		hpanel.add(cancel);
		
		BarButton confirm = new BarButton(confirmLabel);
		confirm.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				callback.confirmed();
				hide();
			}
		});
		hpanel.add(confirm);
		
		setWidget(vpanel);
		
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
}
