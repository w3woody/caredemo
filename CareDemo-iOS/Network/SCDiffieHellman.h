//
//  SCDiffieHellman.h
//  CareDemo
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#ifndef SCDiffieHellman_h
#define SCDiffieHellman_h

#include <stdio.h>
#include "SCBigInteger.h"

/************************************************************************/
/*																		*/
/*	Big Integer Routines												*/
/*																		*/
/************************************************************************/

/*	SCDiffieHellman
 *
 *		Perform the Diffie-Hellman algorithm for private key exchange
 */

class SCDiffieHellman
{
	public:
						SCDiffieHellman();
						~SCDiffieHellman();

		const SCBigInteger	&GetPublicKey() const
							{
								return pubKey;
							}
		SCBigInteger	CalcSharedSecret(const SCBigInteger &bkey) const;

	private:
		SCBigInteger	privKey;
		SCBigInteger	pubKey;
};


#endif /* SCDiffieHellman_hpp */
