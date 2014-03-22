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
    self.assignments = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                        @"1SwipeLeft" ?: [NSNull null], @"Previous",
                        @"1SwipeRight" ?: [NSNull null], @"Next",
                        @"1SwipeUp" ?: [NSNull null], @"VolumeUp",
                        @"1SwipeDown" ?: [NSNull null], @"VolumeDown",
                        @"11Tap" ?: [NSNull null], @"PlayPause",
                        @"12Tap" ?: [NSNull null], @"Unassigned",
                        @"13Tap" ?: [NSNull null], @"Unassigned",
                        @"14Tap" ?: [NSNull null], @"Menu",
                        @"1LongPress" ?: [NSNull null], @"Menu",
                        @"2SwipeLeft" ?: [NSNull null], @"Rewind",
                        @"2SwipeRight" ?: [NSNull null], @"FastForward",
                        @"2SwipeUp" ?: [NSNull null], @"Unassigned",
                        @"2SwipeDown" ?: [NSNull null], @"Unassigned",
                        @"21Tap" ?: [NSNull null], @"PlayPause",
                        @"22Tap" ?: [NSNull null], @"Unassigned",
                        @"23Tap" ?: [NSNull null], @"Unassigned",
                        @"24Tap" ?: [NSNull null], @"Unassigned",
                        @"2LongPress" ?: [NSNull null], @"Unassigned",
                        @"3SwipeLeft" ?: [NSNull null], @"Unassigned",
                        @"3SwipeRight" ?: [NSNull null], @"Unassigned",
                        @"3SwipeUp" ?: [NSNull null], @"Unassigned",
                        @"3SwipeDown" ?: [NSNull null], @"Unassigned",
                        @"31Tap" ?: [NSNull null], @"Unassigned",
                        @"32Tap" ?: [NSNull null], @"Unassigned",
                        @"33Tap" ?: [NSNull null], @"Unassigned",
                        @"34Tap" ?: [NSNull null], @"Unassigned",
                        @"3LongPress" ?: [NSNull null], @"Unassigned",
                        nil];
     return self;
}


@end
