//
//  AppDelegate.m
//  CareDemo
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import "AppDelegate.h"
#import "CDNetwork.h"
#import "SCKeychain.h"

@interface AppDelegate () <CDNetworkDelegate>

@property (strong, nonatomic) UIStoryboard *mainStoryboard;
@property (strong, nonatomic) UIViewController *viewController;

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
	self.mainStoryboard = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
	self.viewController = self.mainStoryboard.instantiateInitialViewController;
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    self.window.backgroundColor = [UIColor whiteColor];
	self.window.rootViewController = self.viewController;
    [self.window makeKeyAndVisible];

	[[CDNetwork shared] setDelegate:self];

	if (!SCHasSecureData()) {
		UIStoryboard *s = [UIStoryboard storyboardWithName:@"Login" bundle:nil];
		UIViewController *vc = [s instantiateInitialViewController];
		[self.viewController presentViewController:vc animated:NO completion:nil];
	}

	return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application {
	// Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
	// Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
	// Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
	// If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
	// Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
	// Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application {
	// Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}


/*
 *	Network delegate
 */

- (NSString *)credentials
{
	if (!SCHasSecureData()) return nil;

	NSData *data = SCGetSecureData();
	if (data == nil) return nil;
	return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

- (void)requestConnectionDialog:(void (^)(BOOL success))callback
{
	UIStoryboard *s = [UIStoryboard storyboardWithName:@"Login" bundle:nil];
	UIViewController *vc = [s instantiateInitialViewController];
	[self.viewController presentViewController:vc animated:YES completion:nil];
}

- (void)showServerError:(CDNetworkResponse *)response
{
	UIAlertController *c = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"Server Error",@"Server Error") message:NSLocalizedString(@"A network problem occurred while making a network request",@"") preferredStyle:UIAlertControllerStyleAlert];

	UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"OK",@"OK") style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
	}];
	[c addAction:action];

	UIViewController *vc = self.viewController;
	while (vc.presentedViewController) {
		vc = vc.presentedViewController;
	}

	[vc presentViewController:c animated:YES completion:nil];
}


@end
