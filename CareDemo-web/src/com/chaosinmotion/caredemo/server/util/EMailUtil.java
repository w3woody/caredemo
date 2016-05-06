/*  EMail.java
 *
 *  Created on Mar 4, 2013 by William Edward Woody
 */

package com.chaosinmotion.caredemo.server.util;

import java.util.LinkedList;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EMailUtil
{
	/*
	 * TODO: Refactor into config properties
	 */
    private static final String FROM = "bugs@glenviewsoftware.com";
    private static final String SMTPHOST = "odin.lunarservers.com";
    private static final String SMTPPORT = "587";
    private static final String USERNAME = "woody@chaosinmotion.com";
    private static final String PASSWORD = "sec3ret";
    private static final String USESSL = "false";
    private static final String USEAUTH = "true";
    
    private static class Message
    {
        private String to;
        private String subject;
        private String body;
        
        Message(String t, String sub, String b)
        {
            to = t;
            subject = sub;
            body = b;
        }
    }
    
    private static class SMTPAuthenticator extends Authenticator
    {
        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication(USERNAME,PASSWORD);
        }
    }
    
    private static class SenderThread implements Runnable
    {
        @Override
        public void run()
        {
            for (;;) {
                try {
                    Message m;
                    
                    synchronized(gQueue) {
                        while (gQueue.isEmpty()) {
                            gQueue.wait();
                        }
                        m = gQueue.removeFirst();
                    }
                    
                    /*
                     * Now send the message
                     */
                    System.out.println("Start");
                    
                    Properties p = new Properties();
                    p.setProperty("mail.smtp.host", SMTPHOST);
                    p.setProperty("mail.smtp.port", SMTPPORT);
                    p.put("mail.smtp.starttls.enable", USESSL);
                    p.setProperty("mail.smtp.auth",USEAUTH);
                    Authenticator auth = new SMTPAuthenticator();
                    Session session = Session.getDefaultInstance(p, auth);
                    
                    MimeMessage msg = new MimeMessage(session);
                    msg.setFrom(new InternetAddress(FROM));
                    msg.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(m.to));
                    msg.setSubject(m.subject);
                    msg.setContent(m.body,"text/plain");
                    
                    Transport.send(msg);
                    
                    System.out.println("Done");
                }
                catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        }
    }
    
    private static LinkedList<Message> gQueue = new LinkedList<Message>();
    private static boolean gIsInit = false;
    
    private static synchronized void initialize()
    {
        if (gIsInit) return;
        gIsInit = true;
        
        Thread th = new Thread(new SenderThread());
        th.setDaemon(true);
        th.setName("Mail Thread");
        th.start();
    }
    /**
     * Enqueues a message to be sent by my background thread
     * @param to
     * @param subject
     * @param body
     */
    public static void sendMessage(String to, String subject, String body)
    {
        initialize();
        
        synchronized(gQueue) {
            gQueue.addLast(new Message(to,subject,body));
            gQueue.notify();
        }
    }
    
    public static void sendResetPassword(String to, String path, String token)
    {
        String body = "You may use the following link to reset your password:\n\n" +
                path + "?token=" + token;
        sendMessage(to,"CareDemo Reset Password",body);
    }
}


