//
//  CDNetworkCookies.m
//  CareDemo
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import "CDNetworkCookies.h"

@interface CDNetworkCookies ()
@property (strong) NSMutableDictionary<NSString *, NSString *> *cookies;
@end

@implementation CDNetworkCookies

- (id)init
{
	if (nil != (self = [super init])) {
		self.cookies = [[NSMutableDictionary alloc] init];
	}
	return self;
}

- (NSString *)sendCookieValue
{
	if (self.cookies.count == 0) return nil;

	NSMutableString *str = [[NSMutableString alloc] init];
	[self.cookies enumerateKeysAndObjectsUsingBlock:^(NSString *key, NSString *obj, BOOL *stop) {
		if ([str length]) {
			[str appendString:@";"];
		}
		[str appendFormat:@"%@=%@",key,obj];
	}];
	return str;
}

- (void)processCookie:(NSString *)str
{
	str = [str stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];

	NSRange range = [str rangeOfString:@";"];	// peel off ';'
	if (range.location != NSNotFound) {
		str = [str substringToIndex:range.location];
		str = [str stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
	}

	/*
	 *	Now we should be left with "name=value"; store as pair.
	 */

	NSString *key;
	NSString *value;
	range = [str rangeOfString:@"="];
	if (range.location != NSNotFound) {
		key = [str substringToIndex:range.location];
		key = [key stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];

		range.location++;
		range.length = str.length - range.location;
		value = [str substringWithRange:range];
		value = [value stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
	} else {
		key = str;
		value = @"";
	}

	/*
	 *	Key/value store
	 */

	self.cookies[key] = value;
}

- (void)processReceivedHeader:(NSDictionary *)headers
{
	NSString *val = headers[@"Set-Cookie"];
	if (val == nil) return;

	/*
	 *	Split across ','
	 */

	for (;;) {
		NSRange range = [val rangeOfString:@","];
		if (range.location != NSNotFound) {
			NSString *front = [val substringToIndex:range.location];
			[self processCookie:front];

			range.location++;
			range.length = val.length - range.location;
			val = [val substringWithRange:range];
		} else {
			[self processCookie:val];
			break;
		}
	}
}

@end
