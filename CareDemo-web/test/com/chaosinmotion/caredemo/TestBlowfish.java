/*	TestBlowfish.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo;

import com.chaosinmotion.caredemo.shared.Blowfish;

/**
 * @author woody
 *
 */
public class TestBlowfish
{
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		Blowfish b;
		String str = "146892836365496756402174056105640209386568282879792624053080236687119916908677830021827175572277695923812512913826212124904383721037481707559841294305245326923446829792640941387021524516139751464664436182291707384172599608810007602318046283144823419838545829192513643568646445782572654560183308501138914355938";
		b = new Blowfish(str.getBytes("UTF-8"));
		
		int[] x = new int[2];
		x[0] = 1;
		x[1] = 2;
		b.encryptBlock(x);
		System.out.println("x: " + x[0] + "," + x[1]);
		
		String txt = "{ \"cmd\": \"test/test\" }";
		byte[] enc = b.encrypt(txt);
		
		boolean start = true;
		for (byte c: enc) {
			if (start) {
				start = false;
			} else {
				System.out.print(',');
			}
			
			System.out.print("(uint8_t)" + Integer.toString(0x00FF & c));
		}
	}
	
}
