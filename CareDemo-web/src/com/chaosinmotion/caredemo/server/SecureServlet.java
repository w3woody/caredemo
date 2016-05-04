/*	SecureServlet.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.chaosinmotion.caredemo.shared.Base64;
import com.chaosinmotion.caredemo.shared.Blowfish;
import com.chaosinmotion.caredemo.shared.DiffieHellman;
import com.chaosinmotion.caredemo.shared.Errors;

/**
 * Our single servlet to handle all requests. This provides a mechanism for
 * a Diffie-Hellman exchange of a secret key for encryption using Blowfish,
 * which may not be the most secure thing in the world, but is far more 
 * secure than just leaving the naked requests floating around. 
 * @author woody
 *
 */
public class SecureServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	private static HashMap<String,Object> interfaces;
	
	public SecureServlet()
	{
		interfaces = new HashMap<String,Object>();
	}
	
	/**
	 * Handle post
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		HttpSession session = req.getSession();

		try {
			/*
			 * Extract the JSON request.
			 */
			JSONTokener tokener = new JSONTokener(req.getInputStream());
			JSONObject requestParams = new JSONObject(tokener);
			
			if (requestParams.has("request")) {
				/*
				 * Extract the request string and decode against our
				 * negotiated private secret key
				 */
				String rstr = requestParams.optString("request");
				
				Blowfish enc = (Blowfish)session.getAttribute("enc");
				if (enc == null) {
					/*
					 * Blowfish key not present; our token may have expired.
					 */
					JSONObject retResult = new JSONObject();
					retResult.put("error", "Token Expired");
					retResult.put("errcode", Errors.TOKENEXPIRED);
					ServletOutputStream stream = resp.getOutputStream();
					resp.setContentType("application/json");
					stream.print(retResult.toString());
				} else {
					
					/*
					 * Decrypt the string.
					 */
					byte[] encrypt = Base64.decode(rstr);
					String str = enc.decrypt(encrypt);
					JSONTokener t = new JSONTokener(str);
					JSONObject request = new JSONObject(t);
					
					/*
					 * At this point we have a decoded request. This should have
					 * a cmd field; the rest serves as the parameters
					 */
					
					if (requestParams.has("cmd")) {
						JSONObject retResult = new JSONObject();
						retResult.put("error", "Command Error");
						retResult.put("errcode", Errors.CMDERROR);
						ServletOutputStream stream = resp.getOutputStream();
						resp.setContentType("application/json");
						stream.print(retResult.toString());
					} else {
						String cmd = request.optString("cmd");

						/*
						 * We have a result. Encrypt using our blowfish key
						 * and send the Base64 data
						 */
						JSONObject result = doRequest(cmd,request,session);
						String resStr = result.toString();
						byte[] encoded = enc.encrypt(resStr);
						String encodedBase64 = Base64.encode(encoded);
						
						/*
						 * Encode the result
						 */
						JSONObject retVal = new JSONObject();
						retVal.put("response", encodedBase64);
						ServletOutputStream stream = resp.getOutputStream();
						resp.setContentType("application/json");
						stream.print(retVal.toString());
					}
				}
			} else if (requestParams.has("pubkey")) {
				/*
				 *  Read the JSON request. This should have a single argument:
				 *  { "pubkey": "number" }
				 *  
				 *  This returns the public key used on the server side, and helps
				 *  to construct the private shared secret
				 */
				
				// Parse the public key from the client
				String pubSecret = requestParams.getString("pubkey");
				
				// Generate the secret from our own public key
				BigInteger bi = new BigInteger(pubSecret);
				DiffieHellman dh = new DiffieHellman();
				BigInteger secret = dh.calcSharedSecret(bi);
				System.out.println("Server: " + secret.toString());
				byte[] secretKey = secret.toByteArray();
				Blowfish blowfish = new Blowfish(secretKey);
				session.setAttribute("enc", blowfish);
				
				// Return our own private key so the client can also generate the
				// secret key
				JSONObject retResult = new JSONObject();
				retResult.put("pubkey", dh.getPublicKey().toString());
				ServletOutputStream stream = resp.getOutputStream();
				resp.setContentType("application/json");
				stream.print(retResult.toString());
			} else {
				/*
				 * Unknown request. Produce result
				 */
				
				JSONObject retResult = new JSONObject();
				retResult.put("error", "Unknown Request");
				retResult.put("errcode", Errors.UNKNOWNREQUEST);
				ServletOutputStream stream = resp.getOutputStream();
				resp.setContentType("application/json");
				stream.print(retResult.toString());
			}
		}
		catch (JSONException exception)
		{
			JSONObject retResult = new JSONObject();
			retResult.put("error", "JSON Error");
			retResult.put("errcode", Errors.JSONERROR);
			ServletOutputStream stream = resp.getOutputStream();
			resp.setContentType("application/json");
			stream.print(retResult.toString());
		}
		catch (Exception exception)
		{
			JSONObject retResult = new JSONObject();
			retResult.put("error", "JSON Error");
			retResult.put("errcode", Errors.SERVEREXCEPTION);
			ServletOutputStream stream = resp.getOutputStream();
			resp.setContentType("application/json");
			stream.print(retResult.toString());
		}
	} 

	/**
	 * Performs the command request provided.
	 * @param cmd Command in the form "group/item"
	 * @param request
	 * @param session 
	 * @return
	 */
	private JSONObject doRequest(String cmd, JSONObject request, HttpSession session) throws Exception
	{
		int index = cmd.indexOf('/');
		String groupName = cmd.substring(0, index);
		String methodName = cmd.substring(index+1);
		
		/*
		 *  Some magic to create our entry point
		 */
		
		String className = "com.chaosinmotion.caredemo.server.commands." + 
				groupName.substring(0, 1).toUpperCase() + groupName.substring(1);
		Object proxy;
		
		synchronized(interfaces) {
			proxy = interfaces.get(className);
			if (proxy == null) {
				Class<?> c = Class.forName(className);
				proxy = c.newInstance();
				interfaces.put(className, proxy);
			}
		}
		
		/*
		 *  With the proxy and the process name, get method and invoke
		 */
		
		Class<?> c = proxy.getClass();
		Method m = c.getMethod(methodName, String.class, JSONObject.class, HttpSession.class);
		
		JSONObject ret = (JSONObject)m.invoke(proxy, cmd, request, session);
		
		return ret;
	};
}
