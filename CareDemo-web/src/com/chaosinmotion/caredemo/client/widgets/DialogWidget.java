/*	DialogPanel.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.widgets;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Extends FlexTable to represent a dialog panel; this contains methods for
 * quickly laying out and setting up the table in a standard way for my
 * stuff.
 * @author woody
 *
 */
public class DialogWidget extends Composite
{
	private FlexTable table;
	private boolean wide;
	
	/**
	 * Initialize widget; if wide, three columns with optional buttons on 
	 * the right.
	 * @param w
	 */
	public DialogWidget(boolean w)
	{
		wide = w;
		
		table = new FlexTable();
		table.setWidth("100%");

		table.setCellPadding(8);
		table.setCellPadding(0);
		table.setBorderWidth(0);
		table.getColumnFormatter().setWidth(0, "120px");
		
		if (w) {
			table.getColumnFormatter().setWidth(2, "50px");
		}
		
		initWidget(table);
	}
	
	public DialogWidget()
	{
		this(false);
	}
	
	/**
	 * Clear the contents
	 */
	public void clear()
	{
		table.removeAllRows();
	}

	/**
	 * Remove row
	 * @param row
	 */
	public void removeRow(int row)
	{
		table.removeRow(row);
	}
	
	/**
	 * Insert row
	 * @param row
	 */
	public void insertRow(int row)
	{
		table.insertRow(row);
	}
	

	/**
	 * Insert a new control widget
	 * @param row
	 * @param title
	 * @param w
	 */
	public void addRow(int row, String title, Widget w)
	{
		table.setText(row, 0, title);
		table.getCellFormatter().setStyleName(row, 0, "dialoglabel");
		
		table.setWidget(row, 1, w);
		
		if (wide) {
			table.getFlexCellFormatter().setColSpan(row, 1, 2);
		}
	}
	
	/**
	 * Add a text box
	 * @param row
	 * @param title
	 * @return
	 */
	public TextBox addTextBox(int row, String title)
	{
		TextBox text = new TextBox();
		text.setStyleName("dialogtextbox");
		
		addRow(row,title,text);
		
		if (wide) {
			table.getFlexCellFormatter().setColSpan(row, 1, 2);
		}
		
		return text;
	}
	
	/**
	 * Add a password text box
	 * @param row
	 * @param title
	 * @return
	 */
	public PasswordTextBox addPasswordTextBox(int row, String title)
	{
		PasswordTextBox text = new PasswordTextBox();
		text.setStyleName("dialogtextbox");
		
		addRow(row,title,text);
		
		if (wide) {
			table.getFlexCellFormatter().setColSpan(row, 1, 2);
		}
		
		return text;
	}
	
	/**
	 * Add a text description that spans both columns
	 */
	
	public void addText(int row, String text)
	{
		table.setText(row, 0, text);
		table.getFlexCellFormatter().setColSpan(row, 0, wide ? 3 : 2);
		table.getCellFormatter().setStyleName(row, 0, "dialogtext");
	}
	
	/**
	 * Add a separator row
	 * @param row
	 */
	public void addSeparator(int row)
	{
		HTML html = new HTML("<hr style=\"width:100%;\"/>");
		table.setWidget(row, 0, html);
		table.getFlexCellFormatter().setColSpan(row, 0, wide ? 3 : 2);
	}
	
	/**
	 * Add a horizontal panel, right aligned, for buttons.
	 * @param row
	 * @return
	 */
	public HorizontalPanel addButtonPanel(int row)
	{
		HorizontalPanel hp = new HorizontalPanel();
		table.getFlexCellFormatter().setColSpan(row, 0, wide ? 3 : 2);
		table.setWidget(row, 0, hp);
		table.getCellFormatter().setHorizontalAlignment(row, 0, HasHorizontalAlignment.ALIGN_RIGHT);
		return hp;
	}
	
	/**
	 * Add widget, returning row for buttons
	 */
	public HorizontalPanel addWideWidget(int row, String title, Widget w)
	{
		HorizontalPanel hr = new HorizontalPanel();
		
		table.setText(row, 0, title);
		table.getCellFormatter().setStyleName(row, 0, "dialoglabel");
		
		table.setWidget(row, 1, w);
		
		table.setWidget(row, 2, hr);
		table.getCellFormatter().setHorizontalAlignment(row, 2, HasHorizontalAlignment.ALIGN_RIGHT);
		
		return hr;
	}
	
	/**
	 * Add widget, returning row for buttons
	 */
	public HorizontalPanel addWideText(int row, String title, String value)
	{
		HorizontalPanel hr = new HorizontalPanel();
		
		table.setText(row, 0, title);
		table.getCellFormatter().setStyleName(row, 0, "dialoglabel");
		
		table.setText(row, 1, value);
		table.getCellFormatter().setStyleName(row, 1, "dialoglabel");
		
		table.setWidget(row, 2, hr);
		table.getCellFormatter().setHorizontalAlignment(row, 2, HasHorizontalAlignment.ALIGN_RIGHT);
		
		return hr;
	}
}
