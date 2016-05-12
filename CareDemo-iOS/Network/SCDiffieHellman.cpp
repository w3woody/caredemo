//
//  SCDiffieHellman.cpp
//  CareDemo
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#include "SCDiffieHellman.h"

/************************************************************************/
/*																		*/
/*	Constants															*/
/*																		*/
/************************************************************************/

/*
 *	Set generator and od values to group 2 (1024 bit) key from RFC 4306
 */

static SCBigInteger G ("2");
static SCBigInteger P ("179769313486231590770839156793787453197860296048756011706444423684197180216158519368947833795864925541502180565485980503646440548199239100050792877003355816639229553136239076508735759914822574862575007425302077447712589550957937778424442426617334727629299387668709205606050270810842907692932019128194467627007");

/*	SCDiffieHellman::SCDiffieHellman
 *
 *		Construct the public and private key
 */

SCDiffieHellman::SCDiffieHellman()
{
	privKey = SCBigInteger::Random(1023);	// random 1023 bit value
	pubKey = G.ModPow(privKey, P);
}

/*	SCDiffieHellman::CalcSharedSecret 
 *
 *		Calculate the shared secret
 */

SCBigInteger SCDiffieHellman::CalcSharedSecret(const SCBigInteger &bkey) const
{
	return bkey.ModPow(privKey, P);
}

