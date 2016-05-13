//
//  CDNetworkRequest.h
//  CareDemo
//
//  Created by William Woody on 5/13/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@class CDNetworkResponse;

@interface CDNetworkRequest : NSObject

@property (strong) NSDictionary *params;
@property (weak) id<NSObject> caller;
@property (copy) void (^callback)(CDNetworkResponse *response);

@property (strong) NSURLSessionDataTask *task;
@property (strong) CDNetworkResponse *lastError;
@property (assign) BOOL waitFlag;

@end
