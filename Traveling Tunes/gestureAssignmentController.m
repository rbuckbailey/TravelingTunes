//
//  gestureAssignmentController.m
//  Traveling Tunes
//
//  Created by buck on 3/10/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#include <stdio.h>
#import "ttunesViewController.h"
#import "gestureAssignmentController.h"

@implementation gestureAssignmentController

- (id)init {
    _singleTap  = PLAYPAUSE;
    _longPress  = MENU;
    _doubleTap  = UNASSIGNED;
    _tripleTap  = UNASSIGNED;
    _swipeLeft  = PREVIOUSRESTART;
    _swipeRight = NEXTSONG;
    _swipeUp    = VOLUMEUP;
    _swipeDown  = VOLUMEDOWN;
    _singleTap2  = UNASSIGNED;
    _longPress2  = UNASSIGNED;
    _doubleTap2  = UNASSIGNED;
    _tripleTap2  = UNASSIGNED;
    _swipeLeft2  = UNASSIGNED;
    _swipeRight2 = UNASSIGNED;
    _swipeUp2    = UNASSIGNED;
    _swipeDown2  = UNASSIGNED;
    _singleTap3  = UNASSIGNED;
    _longPress3  = UNASSIGNED;
    _doubleTap3  = UNASSIGNED;
    _tripleTap3  = UNASSIGNED;
    _swipeLeft3  = UNASSIGNED;
    _swipeRight3 = UNASSIGNED;
    _swipeUp3    = UNASSIGNED;
    _swipeDown3  = UNASSIGNED;
    return self;
}
@end
