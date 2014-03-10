//
//  gestureAssignmentController.h
//  Traveling Tunes
//
//  Created by buck on 3/10/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#ifndef Traveling_Tunes_gestureAssignmentController_h
#define Traveling_Tunes_gestureAssignmentController_h

@interface gestureAssignmentController : NSObject

@property (atomic, strong) NSString *singleTap;
@property (atomic, strong) NSString *doubleTap;
@property (atomic, strong) NSString *longPress;
@property (atomic, strong) NSString *tripleTap;
@property (atomic, strong) NSString *swipeLeft;
@property (atomic, strong) NSString *swipeRight;
@property (atomic, strong) NSString *swipeUp;
@property (atomic, strong) NSString *swipeDown;

@end


#endif
