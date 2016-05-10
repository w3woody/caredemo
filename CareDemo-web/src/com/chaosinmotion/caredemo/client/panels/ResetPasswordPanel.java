/*	UserInfoPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.panels;

import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.client.network.Network;
import com.chaosinmotion.caredemo.client.util.PasswordComplexity;
import com.chaosinmotion.caredemo.client.widgets.BarButton;
import com.chaosinmotion.caredemo.shared.Constants;
import com.chaosinmotion.caredemo.shared.SHA256Hash;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author woody
 *
 */
public class ResetPasswordPanel extends ContentPanel
{
	private PasswordTextBox oldPassword;
	private PasswordTextBox newPassword1;
	private PasswordTextBox newPassword2;

	/**
	 * @param title
	 */
	public ResetPasswordPanel()
	{
		super("Reset Password");

		setContent(initWidget());
	}

	protected Widget initWidget()
	{
		FlexTable table = new FlexTable();
		table.setWidth("100%");
		
		table.setCellPadding(8);
		table.setCellSpacing(0);
		table.setBorderWidth(0);
		
		table.getColumnFormatter().setWidth(0, "120px");
		
		/*
		 * Populate the name, e-mail, and provide buttons for saving
		 */
		
		table.setText(0, 0, "Current Password:");
		table.getCellFormatter().setStyleName(0, 0, "dialoglabel");
		
		oldPassword = new PasswordTextBox();
		oldPassword.setStyleName("dialogtextbox");
		table.setWidget(0, 1, oldPassword);
		
		table.setText(1, 0, "New Password:");
		table.getCellFormatter().setStyleName(1, 0, "dialoglabel");
		
		newPassword1 = new PasswordTextBox();
		newPassword1.setStyleName("dialogtextbox");
		table.setWidget(1, 1, newPassword1);
		
		table.setText(2, 0, "Retype Password:");
		table.getCellFormatter().setStyleName(2, 0, "dialoglabel");
		
		newPassword2 = new PasswordTextBox();
		newPassword2.setStyleName("dialogtextbox");
		table.setWidget(2, 1, newPassword2);
		
		
		table.getFlexCellFormatter().setColSpan(3, 0, 2);
		HorizontalPanel hpanel = new HorizontalPanel();
		table.setWidget(3, 0, hpanel);
		table.getCellFormatter().setAlignment(3, 0, HasHorizontalAlignment.ALIGN_RIGHT, HasVerticalAlignment.ALIGN_MIDDLE);
				
		BarButton barButton = new BarButton("Update Password");
		hpanel.add(barButton);
		barButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event)
			{
				doUpdatePassword();
			}
		});
		
		return table;
	}
	
	private void doUpdatePassword()
	{
		String oldPwd = oldPassword.getText();
		String newPwd = newPassword1.getText();
		String rptPwd = newPassword2.getText();
		
		if (!newPwd.equals(rptPwd)) {
			new MessageBox("Password does not match","Please retype your new password");
			return;
		}
		
		if (!PasswordComplexity.isComplex(newPwd)) {
			new MessageBox("Weak password","Your password must contain an " + 
					"uppercase letter, a lowercase letter, a number and a " + 
					"punctuation mark.");
			return;
		}
		
		oldPwd = SHA256Hash.hash(oldPwd + Constants.SALT);
		newPwd = SHA256Hash.hash(newPwd + Constants.SALT);
		
		JSONObject req = new JSONObject();
		req.put("cmd",new JSONString("users/changePassword"));
		req.put("oldpassword", new JSONString(oldPwd));
		req.put("newpassword", new JSONString(newPwd));
		
		Network.get().request(req, new Network.ResultCallback() {
			@Override
			public void response(JSONObject result)
			{
				new MessageBox("Password updated","Your password was updated.");
				oldPassword.setText("");
				newPassword1.setText("");
				newPassword2.setText("");
			}
			
			@Override
			public void error(int serverError)
			{
				new MessageBox("Unable to update password","Unable to update "
						+ "your password. Please check that the old password "
						+ "was entered correctly.");
				// TODO Auto-generated method stub
				
			}
		});
	}
}
