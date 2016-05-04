/*	DiffieHellman.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.shared;

import java.math.BigInteger;

/**
 * Implements a stripped down verison of the Diffie-Hellman algorithm, for
 * creating a secure token for encoding all exchanges between a client and
 * server.
 * 
 * When this class is generated, it automatically generates a random value
 * and the public key of that random value. When given the public key from
 * the server, this can then generate the resulting internal key, which can
 * then be used as the encryption key for our Blowfish algorithm.
 * 
 * While not perfect, this does help us encrypt the packets that are sent
 * between the client and server.
 * @author woody
 */
public class DiffieHellman
{
	private static final BigInteger G;
	private static final BigInteger P;
	private static final Blowfish secure;
	private static long secureIndex;
	
	private BigInteger privKey;
	private BigInteger pubKey;
	
	static {
		// Set the generator and mod values to the Group 2 (1024 bit)
		// key from RFC 4306
		G = new BigInteger("2");
		P = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF",16);

		long t = System.currentTimeMillis();
		byte[] tb = new byte[8];
		for (int i = 0; i < 8; ++i) {
			tb[i] = (byte)t;
			t >>>= 8;
		}
		secure = new Blowfish(tb);		// use timestamp as key
	}
	
	/**
	 * Internal method for generating a random big integer of 1023 bits.
	 * @return
	 */
	private synchronized static BigInteger random()
	{
		int[] tmp = new int[2];
		byte[] b = new byte[128];
		for (int i = 0; i < 16; ++i) {
			tmp[0] = (int)(secureIndex >> 32);
			tmp[1] = (int)(secureIndex);
			
			secure.encryptBlock(tmp);
			
			b[i + 0] = (byte)(tmp[0] >> 24);
			b[i + 1] = (byte)(tmp[0] >> 16);
			b[i + 2] = (byte)(tmp[0] >> 8);
			b[i + 3] = (byte)(tmp[0]);
			b[i + 4] = (byte)(tmp[1] >> 24);
			b[i + 5] = (byte)(tmp[1] >> 16);
			b[i + 6] = (byte)(tmp[1] >> 8);
			b[i + 7] = (byte)(tmp[1]);
		}
		
		b[0] &= 0x7F;
		return new BigInteger(b);
	}
	
	/**
	 *  Generate a new internal secret key, calculate the modulus
	 */
	public DiffieHellman()
	{
		privKey = random();
		pubKey = G.modPow(privKey, P);
	}
	
	/**
	 * Return our public key to be sent to the other side
	 * @return
	 */
	public BigInteger getPublicKey()
	{
		return pubKey;
	}
	
	/**
	 * Given a public value from the other side, this calculates the
	 * shared secret
	 * @param bkey
	 * @return
	 */
	public BigInteger calcSharedSecret(BigInteger bkey)
	{
		return bkey.modPow(privKey, P);
	}
}
