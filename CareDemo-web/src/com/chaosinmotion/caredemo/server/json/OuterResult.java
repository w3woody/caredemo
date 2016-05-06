/*	OuterResult.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.server.json;

import org.json.JSONObject;
import com.chaosinmotion.caredemo.shared.Base64;
import com.chaosinmotion.caredemo.shared.Blowfish;

/**
 * The outer result object is used by the secure servlet. Normally when we
 * send a result we return our encrypted result wrapped as Base64-encoded
 * JSON. However, if there is a problem (such as the fact that we don't
 * have a key), we need to return JSON indicating an error instead. We also
 * can handle the key exchange JSON return result.
 * @author woody
 *
 */
public class OuterResult
{
	private JSONObject json;
	
	/**
	 * Encrypts the return result with the blowfish encryption key
	 * @param rr
	 * @param enc
	 */
	public OuterResult(ReturnResult rr, Blowfish enc)
	{
		json = new JSONObject();
		
		String rrStr = rr.toString();
		byte[] encoded = enc.encrypt(rrStr);
		String encodedBase64 = Base64.encode(encoded);
		json.put("response", encodedBase64);
	}
	
	/**
	 * Encrypts an error result
	 * @param errorCode
	 * @param errMsg
	 */
	public OuterResult(int errorCode, String errMsg)
	{
		json = new JSONObject();
		json.put("error", errMsg);
		json.put("errcode", errorCode);
	}
	
	/**
	 * Encrypt the key exchange result
	 * @param key
	 */
	public OuterResult(String key)
	{
		json = new JSONObject();
		json.put("pubkey", key);
	}
	
	public String toString()
	{
		return json.toString();
	}
}
