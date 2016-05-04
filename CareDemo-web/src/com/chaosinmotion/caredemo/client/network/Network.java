/*	Network.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.client.network;

import java.math.BigInteger;
import com.chaosinmotion.caredemo.client.dialogs.MessageBox;
import com.chaosinmotion.caredemo.shared.Base64;
import com.chaosinmotion.caredemo.shared.Blowfish;
import com.chaosinmotion.caredemo.shared.DiffieHellman;
import com.chaosinmotion.caredemo.shared.Errors;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.storage.client.Storage;

/**
 * Entry point for our network requests. (TODO: add broadcast mechanism to
 * allow canceling of requests)
 * @author woody
 *
 */
public class Network
{
	private static String getURL()
	{
		String path = GWT.getModuleBaseURL() + "api";
		return path;
	}
	
	/**
	 * Result callback
	 */
	public interface ResultCallback
	{
		void response(JSONObject result);
		void error(int serverError);
		void exception();
	}
	
	public interface SecureCallback
	{
		void success();
		void exception();
	}
	
	private static Network shared;
	private Blowfish encryption;
	
	/**
	 * Get shared network request object.
	 * @return
	 */
	public static synchronized Network get()
	{
		if (shared == null) {
			shared = new Network();
		}
		return shared;
	}
	
	/**
	 * Construct our singleton object. Reconstruct encryption if we ahve
	 * an encryption object in storage. This allows me to continue a
	 * conversation on a separate page without having to renegotiate a
	 * secret key.
	 */
	
	private Network()
	{
		Storage storage = Storage.getSessionStorageIfSupported();
		if (storage != null) {
			String secret = storage.getItem("secret");
			if (secret != null) {
				BigInteger secretInteger = new BigInteger(secret);
				byte[] secretKey = secretInteger.toByteArray();
				encryption = new Blowfish(secretKey);
			}
		}
	}
	
	/**
	 * Send request, in form of json object. This negotiates a secure session
	 * if one hasn't been negotiated yet and wraps our requests.
	 * @param req
	 * @param cb
	 */
	public void request(final JSONObject req, final ResultCallback cb)
	{
		if (encryption == null) {
			/*
			 * Secret key not negotiated yet.
			 */
			
			openSecure(new SecureCallback() {
				@Override
				public void success()
				{
					// Success means we now have an encryption; resend request
					request(req,cb);
				}

				@Override
				public void exception()
				{
					cb.exception();
				}
			});
		} else {
			/*
			 * Encrypt the request.
			 */
			
			String reqStr = req.toString();
			byte[] encrypt = encryption.encrypt(reqStr);
			String encBase64 = Base64.encode(encrypt);
			JSONObject sendReq = new JSONObject();
			sendReq.put("request", new JSONString(encBase64));

			RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,getURL());
			builder.setHeader("Content-Type", "application/json");
			builder.setRequestData(sendReq.toString());
			builder.setCallback(new RequestCallback() {
				@Override
				public void onResponseReceived(Request request,
						Response response)
				{
					/*
					 * Get and decode the response.
					 */
					
					String resp = response.getText();
					JSONObject obj = JSONParser.parseLenient(resp).isObject();
					
					JSONValue err = obj.get("errcode");
					if (err != null) {
						int errCode = (int)(err.isNumber().doubleValue());
						
						/*
						 * Some error codes requre renegotiation. Determine
						 * if renegotiation is requred, and resend
						 */
						
						if ((errCode == Errors.CMDERROR) || 
								(errCode == Errors.JSONERROR) || 
								(errCode == Errors.TOKENEXPIRED)) {
							encryption = null;
							request(req,cb);
						} else {
							/*
							 * These errors are simply passed up
							 */
							
							cb.error(errCode);
						}
						return;
					}
					
					/*
					 * Encode the response.
					 */
					String respStr = obj.get("response").isString().stringValue();
					byte[] respEncode = Base64.decode(respStr);
					String reponseStr = encryption.decrypt(respEncode);
					JSONObject responseData = JSONParser.parseLenient(reponseStr).isObject();
					
					cb.response(responseData);
				}

				@Override
				public void onError(Request request, Throwable exception)
				{
					doError(exception);
					cb.exception();
				}
			});

			// TODO: track requests for canceling?
			try {
				builder.send();
			}
			catch (RequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This attempts to open a secure chat connection. This negotiates an
	 * encryption buffer for sending encrypted requests.
	 * @param res
	 */
	public void openSecure(final SecureCallback res)
	{
		final DiffieHellman dh = new DiffieHellman();
		
		JSONObject req = new JSONObject();
		req.put("pubkey", new JSONString(dh.getPublicKey().toString()));
		
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,getURL());
		builder.setHeader("Content-Type", "application/json");
		builder.setRequestData(req.toString());
		builder.setCallback(new RequestCallback() {
			@Override
			public void onResponseReceived(Request request, Response response)
			{
				String resp = response.getText();
				try {
					JSONObject obj = JSONParser.parseLenient(resp).isObject();
					if (obj == null) {
						new MessageBox("Parser Error","Received incorrect response");
						res.exception();
					} else {
						/*
						 * Calculate our secret key
						 */
						String serverKey = obj.get("pubkey").isString().stringValue();
						BigInteger server = new BigInteger(serverKey);
						BigInteger secret = dh.calcSharedSecret(server);
						
						/*
						 * Save the secret key to session storage
						 */
						Storage storage = Storage.getSessionStorageIfSupported();
						if (storage != null) {
							storage.setItem("secret", secret.toString());
						}
						
						/*
						 * Generate the encryption object
						 */
						byte[] secretKey = secret.toByteArray();
						encryption = new Blowfish(secretKey);
						
						res.success();
					}
				}
				catch (Exception ex) {
					doError(ex);
					res.exception();
					System.out.println();
					System.out.println(resp);
					System.out.println();
				}
			}

			@Override
			public void onError(Request request, Throwable exception)
			{
				doError(exception);
				res.exception();
			}
		});
		
		// TODO: track requests for canceling?
		try {
			builder.send();
		}
		catch (RequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * True if we have a secure connection
	 * @return
	 */
	public boolean isSecure()
	{
		return encryption != null;
	}

	private static void doError(Throwable ex) 
	{
		new MessageBox("Network Exception","Exception: " + ex.getMessage());
	}
}
