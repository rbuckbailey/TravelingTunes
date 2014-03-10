//
//  gestureAssignmentController.m
//  Traveling Tunes
//
//  Created by buck on 3/10/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#include <stdio.h>
#import "ttunesViewController.h"

@implementation gestureAssignmentController

/*
 @synthesize singleTap;

@synthesize doubleTap;
@synthesize tripleTap;
@synthesize swipeLeft;
@synthesize swipeRight;
*/

- (id)init {
    if (self = [super init])
    {
        _singleTap  = @"playPause";
/*    self.doubleTap  = @"doubleTap";
    self.tripleTap  = @"tripleTap";
    self.swipeLeft  = @"previousSong";
    self.swipeRight = @"nextSong";*/
    }
    return self;
}

@end
