//
//  CDNetwork.h
//  CareDemo
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import <Foundation/Foundation.h>

#define ERROR_UNKNOWNREQUEST 			1 
#define ERROR_TOKENEXPIRED 				2 
#define ERROR_CMDERROR 					3 
#define ERROR_JSONERROR 				4 
#define ERROR_EXCEPTION 				5 
#define ERROR_NOTLOGGEDIN 				10 
#define ERROR_MISSINGPARAM 				11 
#define ERROR_INCORRECTCREDENTIALS 		12 
#define ERROR_MOBILEEXPIREDCONNECT 		13 
#define ERROR_INCORRECTMOBILEKEY 		14 
#define ERROR_WRONGPASSWORD 			15 
#define ERROR_ACCESSVIOLATION 			16 
#define ERROR_INCORRECTPARAMS 			17 
#define ERROR_NOSUCHUSER 				18 


@class CDNetworkResponse;
@class CDNetworkResponse;

/*
 *	Network delegate
 */

@protocol CDNetworkDelegate <NSObject>
@optional
- (void)startWaitSpinner;
- (void)stopWaitSpinner;
- (void)showServerError:(CDNetworkResponse *)response;

@required
- (NSString *)credentials;
- (void)requestConnectionDialog:(void (^)(BOOL success))callback;
@end

/*
 *	Network interface
 */

@interface CDNetwork : NSObject

+ (CDNetwork *)shared;

@property (assign) id<CDNetworkDelegate> delegate;

- (BOOL)isSecure;
- (void)openSecure:(void (^)(BOOL success))callback;

- (void)request:(NSDictionary *)reqJSON caller:(id<NSObject>)caller withCallback:(void (^)(CDNetworkResponse *))callback;
- (void)cancelWithCaller:(id<NSObject>)caller;

@end
