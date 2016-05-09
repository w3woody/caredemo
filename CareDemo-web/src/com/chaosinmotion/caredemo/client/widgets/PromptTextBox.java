/*	PromptTextBox.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.widgets;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.TextBox;

/**
 * @author woody
 *
 */
public class PromptTextBox extends TextBox
{
    private String fPrompt;
    private boolean fIsEmpty;
    private boolean fHasFocus;
    
    public PromptTextBox()
    {
        this("");
    }
    
    public PromptTextBox(String prompt)
    {
        super();
        
        fPrompt = prompt;
        if (fPrompt == null) fPrompt = "";
        fIsEmpty = true;
        
        super.setText(fPrompt);
        getElement().getStyle().setProperty("color", "#CCCCCC");
        
        addFocusHandler(new FocusHandler() {
            @Override
            public void onFocus(FocusEvent event)
            {
                if (!fHasFocus) {
                    fHasFocus = true;
                    if (fIsEmpty) {
                        /* Clear the text box. We have focus, so prompt text goes away */
                        PromptTextBox.super.setText("");
                        getElement().getStyle().clearProperty("color");
                    }
                }
            }
        });
        
        addBlurHandler(new BlurHandler() {

            @Override
            public void onBlur(BlurEvent event)
            {
                if (fHasFocus) {
                    fHasFocus = false;
                    String text = PromptTextBox.super.getText();
                    if ((text == null) || (text.length() == 0)) {
                        fIsEmpty = true;
                        PromptTextBox.super.setText(fPrompt);
                        getElement().getStyle().setProperty("color", "#CCCCCC");
                    } else {
                        fIsEmpty = false;
                    }
                }
            }
        });
    }
        
    /**
     * Return the prompt used for this
     */
    public String getPrompt()
    {
        return fPrompt;
    }
    
    /**
     * Change the prompt for this
     * @param prompt
     */
    public void setPrompt(String prompt)
    {
        fPrompt = prompt;
        if (fPrompt == null) fPrompt = "";
        if (fIsEmpty && !fHasFocus) {
            super.setText(fPrompt);
        }
    }
    
    /**
     * Get the text contents
     * @return
     * @see com.google.gwt.user.client.ui.ValueBoxBase#getText()
     */
    public String getText()
    {
        if (fIsEmpty && !fHasFocus) return "";
        return super.getText();
    }
    
    /**
     * Set the text contents
     * Comment
     * @param text
     * @see com.google.gwt.user.client.ui.ValueBoxBase#setText(java.lang.String)
     */
    public void setText(String text)
    {
        if (((text == null) || (text.length() == 0)) && !fHasFocus) {
            /* Clearing the texxt shows the prompt */
            fIsEmpty = true;
            PromptTextBox.super.setText(fPrompt);
            getElement().getStyle().setProperty("color", "#CCCCCC");
        } else {
            /* Setting the text clears the empty flag */
            fIsEmpty = false;
            PromptTextBox.super.setText("");
            getElement().getStyle().clearProperty("color");
        }
    }
}
