/*	BarButton.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.widgets;

import com.google.gwt.user.client.ui.Label;

/**
 * @author woody
 *
 */
public class BarButton extends Label
{
	public BarButton(String text)
	{
		super(text);
		
		setStyleName("barbutton");
	}
}
