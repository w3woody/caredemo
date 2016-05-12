//
//  CDNetworkCookies.h
//  CareDemo
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface CDNetworkCookies : NSObject

- (NSString *)sendCookieValue;
- (void)processReceivedHeader:(NSDictionary *)headers;

@end
