//
//  CDCarePlanStoreManager.m
//  CareDemo
//
//  Created by William Woody on 5/13/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import "CDCarePlanStoreManager.h"

@interface CDCarePlanStoreManager ()
@property (strong) OCKCarePlanStore *planStore;
@end

@implementation CDCarePlanStoreManager

+ (CDCarePlanStoreManager *)shared
{
	static CDCarePlanStoreManager *manager;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		manager = [[CDCarePlanStoreManager alloc] init];
	});
	return manager;
}

- (id)init
{
	if (nil != (self = [super init])) {
		/*
		 *	Initialize
		 */

		NSArray *a = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES);
		NSString *path = a[0];
		NSURL *url = [NSURL fileURLWithPath:path];

		if (![[NSFileManager defaultManager] fileExistsAtPath:path isDirectory:nil]) {
			[[NSFileManager defaultManager] createDirectoryAtURL:url withIntermediateDirectories:YES attributes:nil error:nil];
		}

		self.planStore = [[OCKCarePlanStore alloc] initWithPersistenceDirectoryURL:url];
		self.planStore.delegate = self;

		/*
		 *	TODO: Insights builder?
		 */

		/*
		 *	TODO: Synchronize with API?
		 */

		// TEST: We simply add a daily walk for testing purposes
		NSDateComponents *start = [[NSDateComponents alloc] initWithYear:2016 month:5 day:14];
		OCKCareSchedule *schedule = [OCKCareSchedule weeklyScheduleWithStartDate:start occurrencesOnEachDay:@[ @2, @1, @1, @1, @1, @1, @2 ]];
		OCKCarePlanActivity *walk = [OCKCarePlanActivity interventionWithIdentifier:@"walk01" groupIdentifier:nil title:@"Daily Walk" text:@"15 minutes" tintColor:[UIColor purpleColor] instructions:@"Take a walk" imageURL:nil schedule:schedule userInfo:nil];

		[self.planStore addActivity:walk completion:^(BOOL success, NSError * _Nullable error) {
			// TODO
		}];

		// TEST:: We add an activity for the person to weigh himself
		OCKCarePlanActivity *weight = [OCKCarePlanActivity assessmentWithIdentifier:@"weight01" groupIdentifier:nil title:@"Weight" text:@"Early morning" tintColor:[UIColor redColor] resultResettable:YES schedule:schedule userInfo:nil];
		[self.planStore addActivity:weight completion:^(BOOL success, NSError * _Nullable error) {
			// TODO
		}];
	}
	return self;
}

- (OCKCarePlanStore *)store
{
	return self.planStore;
}

- (NSArray<OCKInsightItem *> *)insights
{
	return [NSArray array];
}

- (NSArray<OCKContact *> *)contacts
{
	return [NSArray array];
}

@end
