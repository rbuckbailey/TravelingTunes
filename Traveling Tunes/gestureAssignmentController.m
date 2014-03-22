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
    _swipeLeft      = PREVIOUSRESTART;
    _swipeRight     = NEXTSONG;
    _swipeUp        = VOLUMEUP;
    _swipeDown      = VOLUMEDOWN;
    _singleTap      = PLAYPAUSE;
    _doubleTap      = MENU;
    _tripleTap      = UNASSIGNED;
    _quadrupleTap   = UNASSIGNED;
    _longPress      = MENU;
    return self;
}
@end
