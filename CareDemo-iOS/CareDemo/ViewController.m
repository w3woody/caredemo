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

@interface ViewController ()
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
//		ORKTaskViewController 
	}
}


@end
