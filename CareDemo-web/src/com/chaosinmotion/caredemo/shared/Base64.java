/*	Base64.java
 * 
 *		CareDemo Copyright 2016 William Edward Woody, all rights reserved.
 */
package com.chaosinmotion.caredemo.shared;

/**
 * Provides a GWT-safe implementation of Base64 encoding/decoding.
 * @author woody
 *
 */
public class Base64
{
    private static final char[] gEncode = {
            'A', 'B', 'C', 'D', 'E',
            'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O',
            'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y',
            'Z', 'a', 'b', 'c', 'd',
            'e', 'f', 'g', 'h', 'i',
            'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's',
            't', 'u', 'v', 'w', 'x',
            'y', 'z', '0', '1', '2',
            '3', '4', '5', '6', '7',
            '8', '9', '+', '/'
        };

    private static int getValue(int ch)
    {
        if ((ch >= 'A') && (ch <= 'Z')) return ch - 'A';
        if ((ch >= 'a') && (ch <= 'z')) return ch - 'a' + 26;
        if ((ch >= '0') && (ch <= '9')) return ch - '0' + 52;
        if (ch == '+') return 62;
        if (ch == '/') return 63;
        if (ch == '=') return -1;
        return -2;
    }
    
    /**
     * Decode string into data.
     * @param base64
     * @return
     */
    public static byte[] decode(String base64)
    {
    	MutableByteArray ret = new MutableByteArray();
    	
        int epos,eval;
        int v;
        int i = 0;
        int len = base64.length();
        
        eval = 0;
        epos = 0;
        while (i < len) {
            v = getValue(base64.charAt(i++));
            if (v == -2) continue;
            if (v == -1) break;
            
            eval |= v << (18 - 6 * epos);
            ++epos;
            
            if (epos >= 4) {
                ret.append(0x00FF & (eval >> 16));
                ret.append(0x00FF & (eval >> 8));
                ret.append(0x00FF & eval);
                
                eval = 0;
                epos = 0;
            }
        }
        
        if (epos >= 2) {
            ret.append(0x00FF & (eval >> 16));
            if (epos >= 3) {
                ret.append(0x00FF & (eval >> 8));
            }
        }
    	
    	return ret.copyData();
    }
    
    /**
     * Encode data into a base64 string with no whitespace
     * @param data
     * @return
     */
    public static String encode(byte[] data)
    {
    	StringBuilder builder = new StringBuilder();
        int epos;
        int eval;
        
        epos = 0;
        eval = 0;
        
        int i = 0;
        int len = data.length;
        while (i < len) {
            eval |= ((0x00FF & data[i++]) << (16 - 8 * epos));
            ++epos;
            
            if (epos >= 3) {
                builder.append(gEncode[0x3F & (eval >> 18)]);
                builder.append(gEncode[0x3F & (eval >> 12)]);
                builder.append(gEncode[0x3F & (eval >> 6)]);
                builder.append(gEncode[0x3F & eval]);
                
                epos = 0;
                eval = 0;
            }
        }
        
        if (epos > 0) {
        	builder.append(gEncode[0x3F & (eval >> 18)]);
        	builder.append(gEncode[0x3F & (eval >> 12)]);
            if (epos > 1) {
            	builder.append(gEncode[0x3F & (eval >> 6)]);
            } else {
            	builder.append('=');
            }
            builder.append('=');
        }
        
        return builder.toString();
    }
}
