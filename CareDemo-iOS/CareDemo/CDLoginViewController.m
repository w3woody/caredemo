//
//  CDLoginViewController.m
//  CareDemo
//
//  Created by William Woody on 5/13/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import "CDLoginViewController.h"
#import "CDNetwork.h"
#import "CDNetworkResponse.h"
#import "SCKeychain.h"
#import "Notifications.h"

@interface CDLoginViewController ()
@property (weak, nonatomic) IBOutlet UILabel *promptLabel;
@property (assign) BOOL inTest;
@property (assign) NSTimer *timer;
@property (copy) NSString *devDescription;
@end

@implementation CDLoginViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

	/*
	 *	Construct description of phone
	 */

	self.devDescription = [NSString stringWithFormat:@"%@ (%@ %@ %@)",
		[[UIDevice currentDevice] name],
		[[UIDevice currentDevice] model],
		[[UIDevice currentDevice] systemName],
		[[UIDevice currentDevice] systemVersion]];

	/*
	 *	Make the API call to get the token, and then start a timer to wait
	 *	for server-side registration to complete
	 */

	NSDictionary *d = @{ @"cmd": @"mobile/getConnectToken" };
	[[CDNetwork shared] request:d caller:self withCallback:^(CDNetworkResponse *r) {
		if (r.success) {
			NSString *token = r.data[@"token"];
			self.promptLabel.text = token;

			[self startTimer];
		}
	}];
}

- (void)dealloc
{
	[[CDNetwork shared] cancelWithCaller:self];
	if (self.timer) {
		[self.timer invalidate];
		self.timer = nil;
	}
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)startTimer
{
	self.timer = [NSTimer scheduledTimerWithTimeInterval:5.0 target:self selector:@selector(testConnect:) userInfo:nil repeats:YES];
}

- (void)testConnect:(NSTimer *)timer
{
	if (self.inTest) return;
	self.inTest = YES;

	NSDictionary *d = @{ @"cmd": @"mobile/pollConnection",
						 @"device": self.devDescription };
	[[CDNetwork shared] request:d caller:self withCallback:^(CDNetworkResponse *r) {
		if (r.success) {
			/*
			 *	We've logged in.
			 */

			BOOL connected = [r.data[@"connected"] boolValue];
			if (connected) {
				NSString *str = r.data[@"token"];
				[self doLoginWithToken:str];
			} else {
				self.inTest = NO;
			}
		} else {
			self.inTest = NO;
		}
	}];
}

- (void)doLoginWithToken:(NSString *)token
{
	NSDictionary *req = @{ @"cmd": @"mobile/login",
						   @"token": token };

	[[CDNetwork shared] request:req caller:self withCallback:^(CDNetworkResponse *r) {
		if (r.success) {
			[self.timer invalidate];
			self.timer = nil;
			
			SCSaveSecureData([token dataUsingEncoding:NSUTF8StringEncoding]);
			[[NSNotificationCenter defaultCenter] postNotificationName:NOTIFICATION_UPDATEUSERINFO object:self userInfo:r.data];

			[self.navigationController dismissViewControllerAnimated:YES completion:nil];
		}
		self.inTest = NO;
	}];
}

@end
