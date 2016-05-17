//
//  ViewController.m
//  CareDemo
//
//  Created by William Woody on 5/12/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import "ViewController.h"
#import "CDCarePlanStoreManager.h"
#import <CareKit/CareKit.h>
#import <ResearchKit/ResearchKit.h>

@interface ViewController ()
@property (strong) OCKSymptomTrackerViewController *symptomTrackerVC;
@end

@implementation ViewController

- (void)viewDidLoad
{
	[super viewDidLoad];


	/*
	 *	Get the store
	 */

	OCKCarePlanStore *store = [[CDCarePlanStoreManager shared] store];
	NSArray<OCKInsightItem *> *insights = [[CDCarePlanStoreManager shared] insights];
	NSArray<OCKContact *> *contacts = [[CDCarePlanStoreManager shared] contacts];

	/*
	 *	Load the care plan view controllers. The way CareKit is designed,
	 *	we need to instantiate and add each of the tabs manually rather than
	 *	rely on the storyboard.
	 */

	OCKCareCardViewController *cc = [[OCKCareCardViewController alloc] initWithCarePlanStore:store];
	UINavigationController *ncc = [[UINavigationController alloc] initWithRootViewController:cc];
	cc.title = NSLocalizedString(@"Care Card", @"Title");
	cc.tabBarItem = [[UITabBarItem alloc] initWithTitle:NSLocalizedString(@"Care Card",@"Title") image:[UIImage imageNamed:@"carecard"] selectedImage:[UIImage imageNamed:@"carecard-filled"]];

	OCKSymptomTrackerViewController *sc = [[OCKSymptomTrackerViewController alloc] initWithCarePlanStore:store];
	sc.delegate = self;
	UINavigationController *nsc = [[UINavigationController alloc] initWithRootViewController:sc];
	sc.title = NSLocalizedString(@"Sympton Tracker", @"Title");
	sc.tabBarItem = [[UITabBarItem alloc] initWithTitle:NSLocalizedString(@"Symptoms",@"Title") image:[UIImage imageNamed:@"symptoms"] selectedImage:[UIImage imageNamed:@"symptoms-filled"]];
	self.symptomTrackerVC = sc;

	// TODO: Determine what sorts of insights I want to give
	OCKInsightsViewController *ic = [[OCKInsightsViewController alloc] initWithInsightItems:insights headerTitle:NSLocalizedString(@"Insights",@"Title") headerSubtitle:nil];
	UINavigationController *nic = [[UINavigationController alloc] initWithRootViewController:ic];
	ic.title = NSLocalizedString(@"Insights", @"Title");
	ic.tabBarItem = [[UITabBarItem alloc] initWithTitle:NSLocalizedString(@"Insights",@"Title") image:[UIImage imageNamed:@"insights"] selectedImage:[UIImage imageNamed:@"insights-filled"]];

	OCKConnectViewController *oc = [[OCKConnectViewController alloc] initWithContacts:contacts];
	UINavigationController *noc = [[UINavigationController alloc] initWithRootViewController:oc];
	oc.title = NSLocalizedString(@"Connect", @"Title");
	oc.tabBarItem = [[UITabBarItem alloc] initWithTitle:NSLocalizedString(@"Connect",@"Title") image:[UIImage imageNamed:@"connect"] selectedImage:[UIImage imageNamed:@"connect-filled"]];

	UIStoryboard *sb = [UIStoryboard storyboardWithName:@"Settings" bundle:nil];
	UINavigationController *vc = sb.instantiateInitialViewController;

	self.viewControllers = @[ ncc, nsc, nic, noc, vc ];
}

- (void)didReceiveMemoryWarning {
	[super didReceiveMemoryWarning];
	// Dispose of any resources that can be recreated.
}


/************************************************************************/
/*																		*/
/*	Symptom tracker														*/
/*																		*/
/************************************************************************/

- (void)symptomTrackerViewController:(OCKSymptomTrackerViewController *)viewController didSelectRowWithAssessmentEvent:(OCKCarePlanEvent *)assessmentEvent
{
	// Test: weight assessment.
	NSString *identifier = assessmentEvent.activity.identifier;
	if ([identifier isEqualToString:@"weight01"]) {
		ORKTaskViewController *vc;

// Build the task for weight. These are the core steps that are being
// performed for weight--which basically create a sequence of steps
// for entering one's weight using Researchkit. (In this case, we create
// a one step questionnaire.)

		id<ORKTask> task;
		ORKHealthKitQuantityTypeAnswerFormat *answerFormat;
		HKQuantityType *type = [HKQuantityType quantityTypeForIdentifier:HKQuantityTypeIdentifierBodyMass];
		answerFormat = [[ORKHealthKitQuantityTypeAnswerFormat alloc] initWithQuantityType:type unit:[HKUnit poundUnit] style:ORKNumericAnswerStyleDecimal];

		ORKQuestionStep *step = [ORKQuestionStep questionStepWithIdentifier:identifier title:@"Input your weight" answer:answerFormat];
		step.optional = NO;
		task = [[ORKOrderedTask alloc] initWithIdentifier:identifier steps:@[ step ]];

		vc = [[ORKTaskViewController alloc] initWithTask:task taskRunUUID:nil];
		vc.delegate = self;

		[self presentViewController:vc animated:YES completion:nil];
	}
}

static HKHealthStore *GetHKHealthStore()
{
	static HKHealthStore *store;

	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		store = [[HKHealthStore alloc] init];
	});
	return store;
}

- (void)taskViewController:(ORKTaskViewController *)taskViewController didFinishWithReason:(ORKTaskViewControllerFinishReason)reason error:(NSError *)error
{
	if (reason == ORKTaskViewControllerFinishReasonCompleted) {
		OCKCarePlanEvent *event = self.symptomTrackerVC.lastSelectedAssessmentEvent;

// Handle the return result from our one step weight questionaire. This basically
// determines that we have a weight event, then stores the results as
// appropriate.

		if ([event.activity.identifier isEqualToString:@"weight01"]) {
			// Build a resulting OCKCarePlanEventResult

			/*
			 *	Get the first result from the list of results. We only have
			 *	one task, so there should only be one result
			 */

			ORKTaskResult *taskResult = taskViewController.result;
			ORKStepResult *firstResult = (ORKStepResult *)taskResult.firstResult;
			ORKResult *stepResult = firstResult.results.firstObject;

			OCKCarePlanEventResult *result;
			if ([stepResult isKindOfClass:[ORKScaleQuestionResult class]]) {
				ORKScaleQuestionResult *scaleResult = (ORKScaleQuestionResult *)stepResult;
				result = [[OCKCarePlanEventResult alloc] initWithValueString:scaleResult.scaleAnswer.stringValue unitString:@"out of 10" userInfo:nil];
			} else if ([stepResult isKindOfClass:[ORKNumericQuestionResult class]]) {
				ORKNumericQuestionResult *numericResult = (ORKNumericQuestionResult *)stepResult;
				result = [[OCKCarePlanEventResult alloc] initWithValueString:numericResult.numericAnswer.stringValue unitString:numericResult.unit userInfo:nil];
			}

			/*
			 *	Now see if we can construct a HealthKit sample
			 *	result to insert into HealthKit.
			 */

			// Construct a HealthKit sample. This parallels our OCKCarePlanEventResult,
			// but formatted for HealthKit.
			HKQuantitySample *hkSample;

			ORKNumericQuestionResult *numericResult = (ORKNumericQuestionResult *)stepResult;

			HKQuantityType *type = [HKQuantityType quantityTypeForIdentifier:HKQuantityTypeIdentifierBodyMass];
			HKQuantity *quantity = [HKQuantity quantityWithUnit:[HKUnit poundUnit] doubleValue:numericResult.numericAnswer.doubleValue];
			NSDate *now = [[NSDate alloc] init];

			hkSample = [HKQuantitySample quantitySampleWithType:type quantity:quantity startDate:now endDate:now];

			NSSet<HKSampleType *> *sampleTypes = [[NSSet alloc] initWithObjects:hkSample.sampleType, nil];

			// See if I can insert into the health kit store
			HKHealthStore *healthStore = GetHKHealthStore();
			OCKCarePlanStore *store = [[CDCarePlanStoreManager shared] store];

			[healthStore requestAuthorizationToShareTypes:sampleTypes readTypes:sampleTypes completion:^(BOOL success, NSError *error) {
				if (success) {
					// We are able to share. Save the assessment
					[healthStore saveObject:hkSample withCompletion:^(BOOL success, NSError * _Nullable error) {
						if (success) {
							// Success. Build an associated result
							// Note we are fudging the unit display string here.
							OCKCarePlanEventResult *assocResult = [[OCKCarePlanEventResult alloc] initWithQuantitySample:hkSample quantityStringFormatter:nil displayUnit:[HKUnit poundUnit] displayUnitStringKey:@"lb" userInfo:nil];

							[store updateEvent:event withResult:assocResult state:OCKCarePlanEventStateCompleted completion:^(BOOL success, OCKCarePlanEvent * _Nullable event, NSError * _Nullable error) {
								if (!success) {
									NSLog(@"Error: %@",error.localizedDescription);
								}
							}];
						} else {
							// Something went haywire; fall back
							[store updateEvent:event withResult:result state:OCKCarePlanEventStateCompleted completion:^(BOOL success, OCKCarePlanEvent *event, NSError *error) {
								if (!success) {
									NSLog(@"Error: %@",error.localizedDescription);
								}
							}];
						}
					}];
				} else {
					// Unable to share. Simply store
					[store updateEvent:event withResult:result state:OCKCarePlanEventStateCompleted completion:^(BOOL success, OCKCarePlanEvent *event, NSError *error) {
						if (!success) {
							NSLog(@"Error: %@",error.localizedDescription);
						}
					}];
				}
			}];
		}
	}

	[self dismissViewControllerAnimated:YES completion:nil];
}

@end
