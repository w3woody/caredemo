/*	MutableByteArray.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.shared;

/**
 * GWT-safe mutable byte array. This creates a mutable array which can then
 * grow as data is appended.
 * @author woody
 */
public class MutableByteArray
{
	private byte[] data;
	private int length;
	private int alloc;
	
	public MutableByteArray(int reserved)
	{
		if (reserved < 32) reserved = 32;
		alloc = reserved;
		length = 0;
		data = new byte[alloc];
	}
	
	public MutableByteArray()
	{
		this(256);
	}
	
	public void clear()
	{
		length = 0;
	}
	
	/**
	 * Resize.
	 * @param resize
	 */
	private void grow(int resize)
	{
		if (resize > alloc) {
			int newsize = (resize * 5)/3;
			byte[] newbuf = new byte[newsize];
			System.arraycopy(data, 0, newbuf, 0, length);
			alloc = newsize;
			data = newbuf;
		}
	}
	
	/**
	 * Append a single byte
	 * @param b
	 */
	public void append(int b)
	{
		if (length >= alloc) {
			grow(alloc+1);
		}
		data[length++] = (byte)b;
	}
	
	/**
	 * Append an array of bytes
	 * @param b
	 */
	public void append(byte[] b)
	{
		append(b,0,b.length);
	}
	
	/**
	 * Append a block of data
	 * @param b
	 * @param start
	 * @param len
	 */
	public void append(byte[] b, int start, int len)
	{
		grow(length + len);	// will grow if this doesn't fit
		System.arraycopy(b, 0, data, length, len);
		length += len;
	}
	
	/**
	 * Allocates a new buffer and returns a copy of the contents that are
	 * stored in memory
	 * @return
	 */
	public byte[] copyData()
	{
		byte[] ret = new byte[length];
		System.arraycopy(data, 0, ret, 0, length);
		return ret;
	}
	
	public byte[] getRawData()
	{
		return data;
	}
	
	public int getLength()
	{
		return length;
	}
}
