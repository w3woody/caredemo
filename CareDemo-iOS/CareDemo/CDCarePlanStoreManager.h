//
//  CDCarePlanStoreManager.h
//  CareDemo
//
//  Created by William Woody on 5/13/16.
//  Copyright © 2016 Glenview Software. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CareKit/CareKit.h>

@interface CDCarePlanStoreManager : NSObject <OCKCarePlanStoreDelegate>

+ (CDCarePlanStoreManager *)shared;

- (OCKCarePlanStore *)store;
- (NSArray<OCKInsightItem *> *)insights;
- (NSArray<OCKContact *> *)contacts;

@end
