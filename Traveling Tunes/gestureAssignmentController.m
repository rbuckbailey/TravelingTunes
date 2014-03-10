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
    _singleTap  = @"playPause";
    _longPress  = @"Long Press";
    _doubleTap  = @"doubleTap";
    _tripleTap  = @"tripleTap";
    _swipeLeft  = @"previousSong";
    _swipeRight = @"nextSong";
    _swipeUp    = @"Volume up";
    _swipeDown  = @"Volume Down";
    return self;
}
@end
