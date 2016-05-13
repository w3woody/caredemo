//
//  CDNetworkResponse.h
//  CareDemo
//
//  Created by William Woody on 5/13/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface CDNetworkResponse : NSObject

/*
 *	HTTP server response
 */

@property (assign) NSInteger serverCode;

/*
 *	Standard response
 */

@property (assign) BOOL success;
@property (assign) NSInteger error;
@property (strong) NSDictionary *data;

/*
 *	Support utilities
 */

- (BOOL)isServerError;
- (BOOL)isAuthenticationError;

@end
