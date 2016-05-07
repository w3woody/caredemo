/*	PatientPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author woody
 *
 */
public class PatientPanel extends ContentPanel
{
	public PatientPanel()
	{
		super("Patient Care Information");
	}

	@Override
	protected Widget initWidget()
	{
		Label l = new Label("This area would contain the patient care information " +
				"that would also be uploaded with CareKit to the back end.");
		l.setWordWrap(true);
		return l;
	}
}
