//
//  CDNetwork.m
//  CareDemo
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import "CDNetwork.h"
#import "CDNetworkCookies.h"
#import "CDNetworkRequest.h"
#import "CDNetworkResponse.h"
#import "Notifications.h"

#include "SCBigInteger.h"
#include "SCBlowfish.h"
#include "SCDiffieHellman.h"

/*
 *	TODO: Change to release 
 */

#define URLPREFIX	@"http://127.0.0.1:8888/caredemo/api"
//#define URLPREFIX	@"http://192.168.1.143:8888/caredemo/api"

/************************************************************************/
/*																		*/
/*	Internal Classes													*/
/*																		*/
/************************************************************************/

@interface CDSecureCallback : NSObject
@property (copy) void (^callback)(BOOL success);
- (id)initWithCallback:(void (^)(BOOL success))callback;
@end

@implementation CDSecureCallback
- (id)initWithCallback:(void (^)(BOOL success))callback
{
	if (nil != (self = [super init])) {
		self.callback = callback;
	}
	return self;
}
@end

/************************************************************************/
/*																		*/
/*	Globals																*/
/*																		*/
/************************************************************************/

@interface CDNetwork ()
{
	SCBlowfish *encryption;
}
@property (strong) NSMutableArray<CDNetworkRequest *> *callQueue;
@property (strong) NSMutableArray<CDNetworkRequest *> *retryQueue;
@property (strong) NSURLSession *session;
@property (strong) CDNetworkCookies *cookies;

@property (assign) BOOL inLogin;
@property (assign) BOOL networkError;

@property (strong) NSMutableArray<CDSecureCallback *> *secureCallback;
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

		self.callQueue = [[NSMutableArray alloc] init];
		self.retryQueue = [[NSMutableArray alloc] init];
		self.secureCallback = nil;

		self.inLogin = NO;
		self.networkError = NO;
	}
	return self;
}

/************************************************************************/
/*																		*/
/*	Send Request Support												*/
/*																		*/
/************************************************************************/

/**
 *	sendRequest:callback:
 *
 *		Perform the standard stuff when we make a request
 */

- (NSURLSessionDataTask *)sendRequest:(NSDictionary *)json callback:(void (^)(int err, NSDictionary *json))callback
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

	if ([self.delegate respondsToSelector:@selector(startWaitSpinner)]) {
		[self.delegate startWaitSpinner];
	}

	// Send request, process response
	NSURLSessionDataTask *task = [self.session dataTaskWithRequest:req completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
		if ([self.delegate respondsToSelector:@selector(stopWaitSpinner)]) {
			[self.delegate stopWaitSpinner];
		}

		NSHTTPURLResponse *urlResponse = (NSHTTPURLResponse *)response;
		[self.cookies processReceivedHeader:urlResponse.allHeaderFields];

		int serverCode = (int)(urlResponse.statusCode);
		NSDictionary *d;
		if (data == nil) {
			d = nil;
		} else {
			d = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
		}

		dispatch_async(dispatch_get_main_queue(), ^{
			copyCallback(serverCode,d);
		});
	}];
	[task resume];

	// Return task reference
	return task;
}


/************************************************************************/
/*																		*/
/*	Secure Handshake													*/
/*																		*/
/************************************************************************/

- (BOOL)isSecure
{
	return (encryption != nil);
}

/**
 *	Open secure connection
 */

- (void)openSecure:(void (^)(BOOL))callback
{
	/*
	 *	Stash callback.
	 */
	if (self.secureCallback) {
		[self.secureCallback addObject:[[CDSecureCallback alloc] initWithCallback:callback]];
		return;
	}
	self.secureCallback = [[NSMutableArray alloc] init];
	[self.secureCallback addObject:[[CDSecureCallback alloc] initWithCallback:callback]];

	/*
	 *	If we get here we perform the handshake.
	 */

	SCDiffieHellman *dh = new SCDiffieHellman;

	NSString *pubKey = [NSString stringWithUTF8String:dh->GetPublicKey().ToString().c_str()];
	NSDictionary *req = @{ @"pubkey": pubKey };

#ifdef DEBUG
	NSLog(@"-> Open Secure");
#endif

	[self sendRequest:req callback:^(int err, NSDictionary *json) {
		/*
		 *	Update our encryption state and call the callbacks
		 */

		if (err == 200) {
#ifdef DEBUG
			NSLog(@"<- Successful open secure");
#endif

			NSString *str = json[@"pubkey"];
			SCBigInteger server([str UTF8String]);
			SCBigInteger secret = dh->CalcSharedSecret(server);
			NSString *skey = [NSString stringWithUTF8String:secret.ToString().c_str()];
			NSData *data = [skey dataUsingEncoding:NSUTF8StringEncoding];

			if (encryption) delete encryption;
			encryption = new SCBlowfish((uint16_t)data.length,(uint8_t *)data.bytes);

			NSArray *a = self.secureCallback;
			self.secureCallback = nil;
			for (CDSecureCallback *cb in a) {
				cb.callback(YES);
			}
		} else {
#ifdef DEBUG
			NSLog(@"<- Unable to handshake");
#endif
			NSArray *a = self.secureCallback;
			self.secureCallback = nil;
			for (CDSecureCallback *cb in a) {
				cb.callback(NO);
			}
		}

		delete dh;
	}];
}

/*
 *	Encrypt a block of data
 */

- (NSData *)encrypt:(NSData *)str
{
	uint32_t *strData;
	uint32_t len = (uint32_t)[str length];
	uint32_t blen = (len + 4 + 7) & ~7;	// length that fits, 8 byte boundary
	uint32_t nwords = blen / 4;

	strData = (uint32_t *)malloc(blen);
	memset(strData,0,blen);

	strData[0] = len;
	memmove(strData+1,str.bytes,str.length);
	for (uint32_t i = 1; i < nwords; ++i) {
		strData[i] = htonl(strData[i]);
	}

	encryption->EncryptData(strData, nwords/2);

	for (uint32_t i = 0; i < nwords; ++i) {
		strData[i] = ntohl(strData[i]);
	}

	return [NSData dataWithBytesNoCopy:strData length:blen freeWhenDone:YES];
}

/*
 *	Decrypt a block of data
 */

- (NSData *)decrypt:(NSData *)data
{
	uint32_t len = (uint32_t)data.length;
	uint32_t *strData = (uint32_t *)malloc(len);
	memmove(strData,data.bytes,len);
	uint32_t nwords = len/4;

	for (int32_t i = 0; i < nwords; ++i) {
		strData[i] = htonl(strData[i]);
	}

	encryption->DecryptData(strData, len / 8);

	for (int32_t i = 1; i < nwords; ++i) {
		strData[i] = ntohl(strData[i]);
	}

	NSData *ret = [NSData dataWithBytes:strData+1 length:*strData];
	free(strData);
	return ret;
}

/************************************************************************/
/*																		*/
/*	Login Request														*/
/*																		*/
/************************************************************************/

typedef enum {
	LOGIN_SUCCESS = 0,
	LOGIN_FAILURE = 1,
	LOGIN_SERVERERROR = 2
} SCLoginError;

- (void)doLogin:(NSString *)creds withCallback:(void (^)(SCLoginError err))callback
{
	void (^copyCallback)(SCLoginError success) = [callback copy];

	NSDictionary *req = @{ @"cmd": @"mobile/login",
						   @"token": creds };
	[self sendRequest:req callback:^(int err, NSDictionary *json) {
		if (err != 200) {
			copyCallback(LOGIN_SERVERERROR);
		} else if ([json[@"success"] boolValue]) {
			[[NSNotificationCenter defaultCenter] postNotificationName:NOTIFICATION_UPDATEUSERINFO object:self userInfo:json[@"data"]];
			copyCallback(LOGIN_SUCCESS);
		} else {
			copyCallback(LOGIN_FAILURE);
		}
	}];
}

/*
 *	We failed to login. Cancel
 */

- (void)failedLogin
{
	self.inLogin = NO;

	for (CDNetworkRequest *req in self.retryQueue) {
		req.callback(req.lastError);
	}
	[self.retryQueue removeAllObjects];
}

/*
 *	Succeeded logging in; retry all the requests that need to be retried
 */

- (void)successfulLogin
{
	self.inLogin = NO;

	NSArray *tmp = [self.retryQueue copy];
	[self.retryQueue removeAllObjects];

	for (CDNetworkRequest *req in tmp) {
		[self sendRequest:req];
	}
}

/*
 *	Run the login dialog. The login dialog will handle the login process
 *	itself and return credentials, or fail and return nil for the credentials
 */

- (void)runLoginDialog
{
	[self.delegate requestConnectionDialog:^(BOOL success) {
		if (success) {
			/*
			 *	We've successfully logged in. Assume the dialog has done
			 *	the heavy lifting of saving the new credentials
			 */

			[self successfulLogin];

		} else {
			/*
			 *	User canceled the login operation. Fail by canceling all
			 *	of the requests, and send cleared credentials
			 */

			[self failedLogin];
		}
	}];
}

- (void)loginRequest
{
	if (self.inLogin) return;
	self.inLogin = YES;

	/*
	 *	Step 1: perform the login request if we have credentials
	 */

	NSString *creds = [self.delegate credentials];
	if (creds) {
		[self doLogin:creds withCallback:^(SCLoginError success) {
			if (success == LOGIN_SUCCESS) {
				[self successfulLogin];
			} else if (success == LOGIN_FAILURE) {
				[self runLoginDialog];
			} else {
				[self failedLogin];
			}
		}];
	} else {
		[self runLoginDialog];
	}
}

/************************************************************************/
/*																		*/
/*	Internal Request Processing											*/
/*																		*/
/************************************************************************/

- (void)enqueueRequest:(CDNetworkRequest *)request
{
#ifdef DEBUG
	NSLog(@"-> req %@",request.params.description);
#endif

	/*
	 *	Encrypt the request and wrap
	 */

	NSData *data = [NSJSONSerialization dataWithJSONObject:request.params options:0 error:nil];
	data = [self encrypt:data];
	NSString *encBase64 = [data base64EncodedStringWithOptions:0];
	NSDictionary *sendReq = @{ @"request": encBase64 };

	request.task = [self sendRequest:sendReq callback:^(int serverCode, NSDictionary *json) {
		[self.callQueue removeObject:request];
		request.task = nil;

		int errcode = [json[@"errcode"] intValue];
		if ((errcode == ERROR_CMDERROR) || (errcode == ERROR_JSONERROR) || (errcode == ERROR_TOKENEXPIRED)) {
#ifdef DEBUG
			NSLog(@"<- encretry");
#endif

			// We need to renegotiate the connection
			delete encryption;
			encryption = NULL;
			[self sendRequest:request];
			return;
		}

		/*
		 *	Formulate response
		 */

		CDNetworkResponse *resp = [[CDNetworkResponse alloc] init];
		resp.serverCode = serverCode;

		if (serverCode != 200) {
			resp.success = NO;
			resp.error = ERROR_EXCEPTION;
			resp.data = nil;
		} else if (errcode) {
			resp.success = NO;
			resp.error = errcode;
			resp.data = nil;
		} else {
			/*
			 *	Decrypt the result
			 */

			NSString *str = json[@"response"];
			NSData *respEncode = [[NSData alloc] initWithBase64EncodedString:str options:NSDataBase64DecodingIgnoreUnknownCharacters];
			NSData *respDecrypt = [self decrypt:respEncode];
			NSDictionary *data = [NSJSONSerialization JSONObjectWithData:respDecrypt options:0 error:nil];

			resp.success = [data[@"success"] boolValue];
			resp.error = [data[@"error"] intValue];
			resp.data = data[@"data"];
		}

#ifdef DEBUG
			NSLog(@"<- reply %@ %@",resp.success ? @"success" : @"error",resp.data.description);
#endif

		if (resp.isAuthenticationError) {
			[self.retryQueue addObject:request];
			request.lastError = resp;
			[self loginRequest];
		} else if (resp.isServerError || !resp.success) {
			if (!self.networkError) {
				self.networkError = YES;
				if ([self.delegate respondsToSelector:@selector(showServerError:)]) {
					[self.delegate showServerError:resp];
				}
			}
		} else {
			self.networkError = NO;
			request.callback(resp);
		}
	}];
}

/*
 *	Internal process request. This enqueues the request and parses the
 *	response.
 */

- (void)sendRequest:(CDNetworkRequest *)request
{
	[self.callQueue addObject:request];

	/*
	 *	Determine if we need to perform a handshake
	 */

	if (encryption == nil) {
		[self openSecure:^(BOOL success) {
			if (success) {
				[self enqueueRequest:request];
			} else {
				/*
				 *	Handshake error. Fail call
				 */

				[self.callQueue removeObject:request];
				CDNetworkResponse *resp = [[CDNetworkResponse alloc] init];
				resp.success = NO;
				resp.serverCode = 0;
				resp.error = ERROR_EXCEPTION;
				resp.data = nil;
				request.callback(resp);
			}
		}];
	} else {
		[self enqueueRequest:request];
	}
}

/************************************************************************/
/*																		*/
/*	Request Processing													*/
/*																		*/
/************************************************************************/

- (void)cancelWithCaller:(id<NSObject>)caller
{
	NSMutableArray<CDNetworkRequest *> *remove = [[NSMutableArray alloc] init];
	for (CDNetworkRequest *req in self.callQueue) {
		if (req.caller == caller) {
			if (req.task) {
				[req.task cancel];
			}
			[remove addObject:req];

			if ([self.delegate respondsToSelector:@selector(stopWaitSpinner)] && req.waitFlag) {
				req.waitFlag = NO;
				[self.delegate stopWaitSpinner];
			}
		}
	}
	[self.callQueue removeObjectsInArray:remove];

	remove = [[NSMutableArray alloc] init];
	for (CDNetworkRequest *req in self.retryQueue) {
		if (req.caller == caller) {
			if (req.task) {
				[req.task cancel];
			}
			[remove addObject:req];

			if ([self.delegate respondsToSelector:@selector(stopWaitSpinner)] && req.waitFlag) {
				req.waitFlag = NO;
				[self.delegate stopWaitSpinner];
			}
		}
	}
	[self.retryQueue removeObjectsInArray:remove];
}

- (void)request:(NSDictionary *)reqJSON caller:(id<NSObject>)caller withCallback:(void (^)(CDNetworkResponse *))callback
{
	CDNetworkRequest *req = [[CDNetworkRequest alloc] init];
	req.params = reqJSON;
	req.caller = caller;
	req.callback = callback;

	[self sendRequest:req];
}



@end
