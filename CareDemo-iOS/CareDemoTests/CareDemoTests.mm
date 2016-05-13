//
//  CareDemoTests.m
//  CareDemoTests
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import <XCTest/XCTest.h>
#import "SCBlowfish.h"

@interface CareDemoTests : XCTestCase

@end

@implementation CareDemoTests

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

static NSData *encrypt(NSData *str, SCBlowfish *encryption)
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

- (void)testExample
{
	NSString *token = @"146892836365496756402174056105640209386568282879792624053080236687119916908677830021827175572277695923812512913826212124904383721037481707559841294305245326923446829792640941387021524516139751464664436182291707384172599608810007602318046283144823419838545829192513643568646445782572654560183308501138914355938";
	NSData *data = [token dataUsingEncoding:NSUTF8StringEncoding];
	SCBlowfish b((uint16_t)[data length],(uint8_t *)[data bytes]);

	uint32_t x[2];
	x[0] = 1;
	x[1] = 2;
	b.EncryptBlock(x);

	XCTAssert(x[0] == 742246052);
	XCTAssert(x[1] == -1571046149);

	NSString *test = @"{ \"cmd\": \"test/test\" }";
	data = [test dataUsingEncoding:NSUTF8StringEncoding];
	NSData *enc = encrypt(data,&b);
	uint8_t cmp[] = { (uint8_t)92,(uint8_t)95,(uint8_t)60,(uint8_t)139,
				 	  (uint8_t)70,(uint8_t)104,(uint8_t)226,(uint8_t)146,
					  (uint8_t)54,(uint8_t)117,(uint8_t)233,(uint8_t)210,
					  (uint8_t)234,(uint8_t)246,(uint8_t)197,(uint8_t)85,
					  (uint8_t)248,(uint8_t)115,(uint8_t)52,(uint8_t)244,
					  (uint8_t)79,(uint8_t)90,(uint8_t)97,(uint8_t)193,
					  (uint8_t)52,(uint8_t)137,(uint8_t)130,(uint8_t)44,
					  (uint8_t)76,(uint8_t)39,(uint8_t)179,(uint8_t)7 };
	int i,len = enc.length;
	uint8_t *bytes = (uint8_t *)enc.bytes;
	for (i = 0; i < len; ++i) {
		XCTAssert(cmp[i] == bytes[i]);
	}
}

@end
