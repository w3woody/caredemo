/*	SHA256Hash.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.shared;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * SHA-256 implementation which compiles on GWT
 * @author woody
 *
 */
public class SHA256Hash
{
	private long total;
	private int index;
	private int[] state;
	private byte[] buffer;
	
	private static final int[] KVal = {
			0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
			0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
			0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
			0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
			0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
			0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
			0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
			0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
			0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
			0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
			0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
			0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
			0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
			0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
			0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
			0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
	};
	
	/**
	 * SHA256 construction
	 */
	public SHA256Hash()
	{
		state = new int[8];
		buffer = new byte[64];
	}
		
	/*
	 * Internal methods
	 */

	private int SHR(int x, int n)
	{
		return x >>> n;
	}

	private int RTR(int x, int n)
	{
		return SHR(x,n) | (int)(x << (32 - n));
	}

	/*
	 *	4.1.2: SHA_256 functions
	 */

	private int sum0(int x)
	{
		return RTR(x, 2) ^ RTR(x, 13) ^ RTR(x, 22);
	}

	private int sum1(int x)
	{
		return RTR(x, 6) ^ RTR(x, 11) ^ RTR(x, 25);
	}

	private int sigma0(int x)
	{
		return RTR(x, 7) ^ RTR(x, 18) ^ SHR(x, 3);
	}

	private int sigma1(int x)
	{
		return RTR(x, 17) ^ RTR(x, 19) ^ SHR(x, 10);
	}

	private int ch(int x, int y, int z)
	{
		return (x & y) ^ (~x & z);
	}

	private int maj(int x, int y, int z)
	{
		return (x & y) ^ (x & z) ^ (y & z);
	}
	
	/**
	 * Start
	 */
	public void start()
	{
		Arrays.fill(buffer, (byte)0);
		total = 0;
		index = 0;
		
		// start state vector for SHA-256
		state[0] = 0x6A09E667;
		state[1] = 0xBB67AE85;
		state[2] = 0x3C6EF372;
		state[3] = 0xA54FF53A;
		state[4] = 0x510E527F;
		state[5] = 0x9B05688C;
		state[6] = 0x1F83D9AB;
		state[7] = 0x5BE0CD19;
	}
	
	/**
	 *		Given the buffer contents (in the buffer object), run the inner
	 *	loop of the SHA-256 calculation
	 */

	private void processBuffer()
	{
		/*
		 *	Step 1: unroll buffer into array, and initialize w
		 *
		 *	(Algorithm 6.2.2)
		 */

		int[] w = new int[64];
		for (int i = 0; i < 16; ++i) {
			int tmp = 0x0FF & buffer[i*4];
			tmp = (tmp << 8) | (0x00FF & buffer[i*4+1]);
			tmp = (tmp << 8) | (0x00FF & buffer[i*4+2]);
			tmp = (tmp << 8) | (0x00FF & buffer[i*4+3]);
			w[i] = tmp;
		}
		for (int i = 16; i < 64; ++i) {
			w[i] = sigma1(w[i-2]) + w[i-7] + sigma0(w[i-15]) + w[i-16];
		}

		// step 2
		int a = state[0];
		int b = state[1];
		int c = state[2];
		int d = state[3];
		int e = state[4];
		int f = state[5];
		int g = state[6];
		int h = state[7];

		for (int i = 0; i < 64; ++i) {
			int t1 = h + sum1(e) + ch(e,f,g) + KVal[i] + w[i];
			int t2 = sum0(a) + maj(a, b, c);
			h = g;
			g = f;
			f = e;
			e = d + t1;
			d = c;
			c = b;
			b = a;
			a = t1 + t2;
		}

		state[0] += a;
		state[1] += b;
		state[2] += c;
		state[3] += d;
		state[4] += e;
		state[5] += f;
		state[6] += g;
		state[7] += h;
	}

	/*	SCSHA256Context::Update
	 *
	 *		Update--append message
	 */

	public void update(byte[] data)
	{
		int wlen;
		int len = data.length;
		int pos = 0;
		total += len;

		/*
		 *	Pull the chunks across, encoding each type
		 */

		while (pos < len) {
			wlen = len - pos;
			if (wlen > 64 - index) wlen = 64 - index;
			System.arraycopy(data, pos, buffer, index, wlen);
			index += wlen;
			
			if (index >= 64) {
				processBuffer();
				index = 0;
			}

			pos += wlen;
		}
	}

	/*	SCSHA256Context::Finish
	 *
	 *		Finish processing, return result.
	 */

	public byte[] finish()
	{
		byte[] tail = { (byte)0x80 };

		/*
		 *	Step 1: append 0x80 to string
		 */

		long curLen = total;
		update(tail);

		/*
		 *	Step 2: append size in bits
		 */

		curLen *= 8;		// 8 bits per byte
		if (index > 56) {	// If not fits, zero and add next block
			Arrays.fill(buffer, index, buffer.length, (byte)0);
			processBuffer();
			index = 0;
		}
		Arrays.fill(buffer, index, 56 - index, (byte)0);
		for (int i = 0; i < 8; ++i) {
			buffer[63-i] = (byte)curLen;
			curLen >>= 8;
		}
		processBuffer();

		/*
		 *	Convert result state into output
		 */

		byte[] output = new byte[32];
		for (int i = 0; i < 8; ++i) {
			int tmp = state[i];
			output[i*4+3] = (byte)tmp;
			tmp >>>= 8;
			output[i*4+2] = (byte)tmp;
			tmp >>>= 8;
			output[i*4+1] = (byte)tmp;
			tmp >>>= 8;
			output[i*4] = (byte)tmp;
		}
		
		return output;
	}
	
	private static char hexToChar(int i)
	{
		i &= 0x0F;
		
		if ((i >= 0) && (i <= 9)) return (char)(i + '0');
		return (char)(i + 'A' - 10);
	}

	/**
	 * Simplified interface which generates a string hash given a string
	 * input.
	 * @param input
	 * @return
	 */
	public static String hash(String input)
	{
		try {
			byte[] data = input.getBytes("UTF-8");
			
			SHA256Hash hash = new SHA256Hash();
			hash.update(data);
			byte[] result = hash.finish();
			
			StringBuilder builder = new StringBuilder();
			for (byte b: result) {
				builder.append(hexToChar(b >>> 4)).append(hexToChar(b));
			}
			return builder.toString();
		}
		catch (UnsupportedEncodingException e) {
			// Should never happen
			return "";
		}
	}
}
