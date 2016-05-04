/*	SecureChat: A secure chat system which permits secure communications 
 *  between iOS devices and a back-end server.
 *
 *	Copyright © 2016 by William Edward Woody
 *
 *	This program is free software: you can redistribute it and/or modify it 
 *	under the terms of the GNU General Public License as published by the 
 *	Free Software Foundation, either version 3 of the License, or (at your 
 *	option) any later version.
 *
 *	This program is distributed in the hope that it will be useful, but 
 *	WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *	or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *	for more details.
 *
 *	You should have received a copy of the GNU General Public License along 
 *	with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.chaosinmotion.caredemo.shared;

/**
 * Gives the list of error codes that can be returned by the server. If
 * the return result is in error, an error field will be returned which
 * gives one of the following error codes. An error code field is not 
 * present if there is no error.
 */
public interface Errors
{
	/**
	 * Unknown request sent to back end
	 */
	public int UNKNOWNREQUEST = 1;
	
	/**
	 * Token expired. This means we no longer are holding the session token
	 * for encrypting and decrypting requests, and the client will need
	 * to renegotiate (and sign back in).
	 */
	public int TOKENEXPIRED = 2;
	
	/**
	 * Command error. This can happen if there was a problem decoding
	 * the command or if the "cmd" field was not provided.
	 */
	public int CMDERROR = 3;
	
	/**
	 * JSON format error. Can happen if the JSON could not be parsed; this
	 * may be because the encryption token was wrong.
	 */
	public int JSONERROR = 4;

	/**
	 * Encrypted result; indicates command in the request was unknown.
	 */
	public int UNKNOWNCOMMAND = 5;

	/**
	 * Internal server-side exception
	 */
	public int ERROR_EXCEPTION = 10;

	public int SERVEREXCEPTION = 11;
}
