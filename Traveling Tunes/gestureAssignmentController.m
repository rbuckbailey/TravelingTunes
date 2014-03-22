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

    
    [_assignments setObject:@"Unassigned" forKey:@"1SwipeLeft"];
    [_assignments setObject:@"Unassigned" forKey:@"1SwipeRight"];
    [_assignments setObject:@"Unassigned" forKey:@"1SwipeUp"];
    [_assignments setObject:@"Unassigned" forKey:@"1SwipeDown"];
    [_assignments setObject:@"PlayPause" forKey:@"11Tap"];
    [_assignments setObject:@"Menu" forKey:@"12Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"13Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"14Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"LongPress"];
    [_assignments setObject:@"Unassigned" forKey:@"2SwipeLeft"];
    [_assignments setObject:@"Unassigned" forKey:@"2SwipeRight"];
    [_assignments setObject:@"Unassigned" forKey:@"2SwipeUp"];
    [_assignments setObject:@"Unassigned" forKey:@"2SwipeDown"];
    [_assignments setObject:@"Unassigned" forKey:@"21Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"22Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"23Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"24Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"2LongPress"];
    [_assignments setObject:@"Unassigned" forKey:@"3SwipeLeft"];
    [_assignments setObject:@"Unassigned" forKey:@"3SwipeRight"];
    [_assignments setObject:@"Unassigned" forKey:@"3SwipeUp"];
    [_assignments setObject:@"Unassigned" forKey:@"3SwipeDown"];
    [_assignments setObject:@"Unassigned" forKey:@"31Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"32Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"33Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"34Tap"];
    [_assignments setObject:@"Unassigned" forKey:@"3LongPress"];
     return self;
}
@end
