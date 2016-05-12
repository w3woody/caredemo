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


@interface CDNetwork : NSObject

+ (CDNetwork *)shared;

- (BOOL)isSecure;
- (void)openSecure:(void (^)(BOOL success))callback;

- (void)request:(NSDictionary *)reqJSON withCallback:(void (^)(int err, NSDictionary *result))callback;

@end
