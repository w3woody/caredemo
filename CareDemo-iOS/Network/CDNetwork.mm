//
//  CDNetwork.m
//  CareDemo
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import "CDNetwork.h"
#import "CDNetworkCookies.h"

#include "SCBigInteger.h"
#include "SCBlowfish.h"
#include "SCDiffieHellman.h"

/*
 *	TODO: Change to release 
 */

#define URLPREFIX	@"http://127.0.0.1:8888/caredemo/api"

@interface CDNetwork ()
{
	SCBlowfish *encryption;
}
@property (strong) NSURLSession *session;
@property (strong) CDNetworkCookies *cookies;
@end

@implementation CDNetwork

/************************************************************************/
/*																		*/
/*	Startup/Shutdown													*/
/*																		*/
/************************************************************************/

+ (CDNetwork *)shared
{
	static CDNetwork *network;

	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		network = [[CDNetwork alloc] init];
	});
	return network;
}

- (id)init
{
	if (nil != (self = [super init])) {
		NSURLSessionConfiguration *config = [NSURLSessionConfiguration ephemeralSessionConfiguration];
		self.cookies = [[CDNetworkCookies alloc] init];
		self.session = [NSURLSession sessionWithConfiguration:config];
	}
	return self;
}

/**
 *	sendRequest:callback:
 *
 *		Perform the standard stuff when we make a request
 */

- (void)sendRequest:(NSDictionary *)json callback:(void (^)(int err, NSDictionary *json))callback
{
	void (^copyCallback)(int err, NSDictionary *d) = [callback copy];

	// Build request
	NSURL *url = [NSURL URLWithString:URLPREFIX];
	NSMutableURLRequest *req = [[NSMutableURLRequest alloc] initWithURL:url];
	[req setHTTPMethod:@"POST"];
	NSData *data = [NSJSONSerialization dataWithJSONObject:json options:0 error:nil];
	[req setHTTPBody:data];
	NSString *cookie = [self.cookies sendCookieValue];
	if (cookie) [req setValue:cookie forHTTPHeaderField:@"Cookie"];

	// Send request, process response
	[self.session dataTaskWithRequest:req completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {

		NSHTTPURLResponse *urlResponse = (NSHTTPURLResponse *)response;
		int serverCode = (int)(urlResponse.statusCode);
		NSDictionary *d;
		if (data == nil) {
			d = nil;
		} else {
			d = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
		}

		copyCallback(serverCode,d);
	}];
}


- (BOOL)isSecure
{
	return (encryption != nil);
}

/**
 *	Open secure connection
 */

- (void)openSecure:(void (^)(BOOL))callback
{
	SCDiffieHellman *dh = new SCDiffieHellman;
	void (^copyCallback)(BOOL f) = [callback copy];

	NSString *pubKey = [NSString stringWithUTF8String:dh->GetPublicKey().ToString().c_str()];
	NSDictionary *req = @{ @"pubkey": pubKey };

	[self sendRequest:req callback:^(int err, NSDictionary *json) {
		if (err == 200) {
			NSString *str = json[@"pubkey"];
			SCBigInteger server([str UTF8String]);
			SCBigInteger secret = dh->CalcSharedSecret(server);
			NSString *skey = [NSString stringWithUTF8String:secret.ToString().c_str()];
			NSData *data = [skey dataUsingEncoding:NSUTF8StringEncoding];

			if (encryption) delete encryption;
			encryption = new SCBlowfish((uint16_t)data.length,(uint8_t *)data.bytes);

			copyCallback(YES);
		} else {
			NSLog(@"Unable to handshake");
			copyCallback(NO);
		}

		delete dh;
	}];
}

- (NSData *)encrypt:(NSData *)str
{
	uint32_t *strData;
	uint32_t len = (uint32_t)[str length];
	uint32_t blen = (len + 4 + 7) & ~7;	// length that fits, 8 byte boundary
	uint32_t nwords = blen / 4;

	strData = (uint32_t *)malloc(blen);
	strData[0] = len;
	memmove(strData+1,str.bytes,str.length);
	for (uint32_t i = 1; i < nwords; ++i) {
		strData[i] = htonl(strData[i]);
	}

	encryption->EncryptData(strData, nwords*2);

	return [NSData dataWithBytesNoCopy:strData length:blen freeWhenDone:YES];
}

- (NSData *)decrypt:(NSData *)data
{
	uint32_t len = (uint32_t)data.length;
	uint32_t *strData = (uint32_t *)malloc(len);
	memmove(strData,data.bytes,len);

	encryption->DecryptData(strData, len / 8);

	len /= 4;
	for (int32_t i = 1; i < len; ++i) {
		strData[i] = ntohl(strData[i]);
	}

	NSData *ret = [NSData dataWithBytes:strData+1 length:*strData];
	free(strData);
	return ret;
}

/**
 *	Make encrypted request
 */

- (void)request:(NSDictionary *)reqJSON withCallback:(void (^)(int err, NSDictionary *result))callback
{
	void (^copyCallback)(int err, NSDictionary *d) = [callback copy];

	if (encryption == NULL) {
		[self openSecure:^(BOOL success) {
			if (success) {
				// redo request with opened connection
				[self request:reqJSON withCallback:copyCallback];
			} else {
				copyCallback(ERROR_EXCEPTION,nil);
			}
		}];
	} else {

		/*
		 *	Encrypt the request and wrap
		 */

		NSData *data = [NSJSONSerialization dataWithJSONObject:reqJSON options:0 error:nil];
		data = [self encrypt:data];
		NSString *encBase64 = [data base64EncodedStringWithOptions:0];
		NSDictionary *sendReq = @{ @"request": encBase64 };

		[self sendRequest:sendReq callback:^(int err, NSDictionary *json) {
			if (err == 200) {
				/*
				 *	Get and decode the response
				 */

				NSNumber *err = json[@"errcode"];
				if (err) {
					int errCode = err.intValue;

					// If the error suggests we have the wrong private key,
					// rerun after deleting the encryption key.
					if ((errCode == ERROR_CMDERROR) || (errCode == ERROR_JSONERROR) ||
							(errCode == ERROR_TOKENEXPIRED)) {
						delete encryption;
						encryption = NULL;
						[self request:reqJSON withCallback:copyCallback];

					} else {
						callback(errCode,json);
					}
					return;
				}

				/*
				 *	Decrypt
				 */

				NSString *str = json[@"response"];
				NSData *respEncode = [[NSData alloc] initWithBase64EncodedString:str options:NSDataBase64DecodingIgnoreUnknownCharacters];

				NSData *respDecrypt = [self decrypt:respEncode];
				NSDictionary *data = [NSJSONSerialization JSONObjectWithData:respDecrypt options:0 error:nil];

				/*
				 *	Determine if we were successful
				 */

				if ([data[@"success"] boolValue]) {
					copyCallback(0,data);
				} else {
					/*
					 *	TODO: Handle logging in with device token if we
					 *	aren't logged in.
					 */
					
					int errCode = [data[@"error"] intValue];
					copyCallback(errCode,data);
				}
			} else {
				NSLog(@"Error");
				copyCallback(ERROR_EXCEPTION,nil);
			}
		}];
	}
}


@end
