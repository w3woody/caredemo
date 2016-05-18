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
@property (strong) NSArray<OCKInsightItem *> *insightList;
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
		 *	Insights builder?
		 */

		self.insightList = @[];

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
	return self.insightList;
}

- (NSArray<OCKContact *> *)contacts
{
	CNPhoneNumber *phone = [[CNPhoneNumber alloc] initWithStringValue:@"888-555-1111"];
	CNPhoneNumber *msg = [[CNPhoneNumber alloc] initWithStringValue:@"888-555-1112"];
	OCKContact *contact = [[OCKContact alloc] initWithContactType:OCKContactTypeCareTeam name:@"John Appleseed" relation:@"Physician" tintColor:[UIColor blueColor] phoneNumber:phone messageNumber:msg emailAddress:@"jappleseed@me.com" monogram:@"JA" image:nil];
	return @[ contact ];
}

/*
 *	This rebuilds the insights and broadcasts a message indicating our
 *	list of insights have been updated.
 */

- (void)updateInsights
{
	// Calculate a one week range
	NSDate *now = [NSDate date];
	NSCalendar *calendar = [NSCalendar currentCalendar];

	NSDateComponents *end = [[NSDateComponents alloc] initWithDate:now calendar:calendar];
	NSDateComponents *start = [[NSDateComponents alloc] initWithDate:[now dateByAddingTimeInterval:-604800] calendar:calendar];	// subtract 1 week

	// Enumerate events for activity
	[self.planStore activityForIdentifier:@"weight01" completion:^(BOOL success, OCKCarePlanActivity *activity, NSError *error) {

		NSMutableDictionary<NSDateComponents *, OCKCarePlanEvent *> *map = [[NSMutableDictionary alloc] init];
		[self.planStore enumerateEventsOfActivity:activity startDate:start endDate:end handler:^(OCKCarePlanEvent *event, BOOL *stop) {
			map[event.date] = event;
		} completion:^(BOOL completed, NSError *error) {
			if (error) {
				NSLog(@"Error: %@",error.localizedDescription);
			}

			if (completed) {
				NSArray *sortedKeys = [map.allKeys sortedArrayUsingComparator:^NSComparisonResult(NSDateComponents *obj1, NSDateComponents *obj2) {
					NSTimeInterval t1 = obj1.date.timeIntervalSinceReferenceDate;
					NSTimeInterval t2 = obj2.date.timeIntervalSinceReferenceDate;

					if (t1 < t2) return NSOrderedAscending;
					else if (t1 > t2) return NSOrderedDescending;
					else return NSOrderedSame;
				}];

				NSMutableArray<NSNumber *> *values = [[NSMutableArray alloc] init];
				NSMutableArray<NSString *> *labels = [[NSMutableArray alloc] init];
				for (NSDateComponents *c in sortedKeys) {
					OCKCarePlanEvent *e = map[c];
					if (e.result.valueString) {
						[values addObject:@(e.result.valueString.doubleValue)];
						[labels addObject:[e.result.valueString copy]];
					}
				}

				OCKBarSeries *s = [[OCKBarSeries alloc] initWithTitle:@"Weight" values:values valueLabels:labels tintColor:[UIColor blueColor]];

				OCKBarChart *chart = [[OCKBarChart alloc] initWithTitle:@"Weight" text:nil tintColor:[UIColor purpleColor] axisTitles:@[ @"axistitle" ] axisSubtitles:@[ @"axissubtitles" ] dataSeries:@[ s ]];

				dispatch_async(dispatch_get_main_queue(), ^{
					self.insightList = @[ chart ];
					[[NSNotificationCenter defaultCenter] postNotificationName:NOTIFICATION_INSIGHTSUPDATED object:self];
				});
			}
		}];
	}];
}

- (void)carePlanStoreActivityListDidChange:(OCKCarePlanStore *)store
{
	[self updateInsights];
	// TODO: Queue events for sending to back end API?
}

- (void)carePlanStore:(OCKCarePlanStore *)store didReceiveUpdateOfEvent:(OCKCarePlanEvent *)event
{
	[self updateInsights];
	// TODO: Queue events for sending to back end API?
}

@end
