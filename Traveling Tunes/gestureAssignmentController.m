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


- (void)resetAssignments {
     // this configures array to defaults.
     self.assignments = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
     @"Previous" ?: [NSNull null], @"1SwipeLeft",
     @"Next" ?: [NSNull null], @"1SwipeRight",
     @"VolumeUp" ?: [NSNull null], @"1SwipeUp",
     @"VolumeDown" ?: [NSNull null], @"1SwipeDown",
     @"PlayPause" ?: [NSNull null], @"11Tap",
     @"Unassigned" ?: [NSNull null], @"12Tap",
     @"Unassigned" ?: [NSNull null], @"13Tap",
     @"Menu" ?: [NSNull null], @"14Tap",
     @"Menu" ?: [NSNull null], @"1LongPress",
     @"Rewind" ?: [NSNull null], @"2SwipeLeft",
     @"FastForward" ?: [NSNull null], @"2SwipeRight",
     @"Unassigned" ?: [NSNull null], @"2SwipeUp",
     @"Unassigned" ?: [NSNull null], @"2SwipeDown",
     @"Unassigned" ?: [NSNull null], @"21Tap",
     @"Unassigned" ?: [NSNull null], @"22Tap",
     @"Unassigned" ?: [NSNull null], @"23Tap",
     @"Unassigned" ?: [NSNull null], @"24Tap",
     @"Unassigned" ?: [NSNull null], @"2LongPress",
     @"Unassigned" ?: [NSNull null], @"3SwipeLeft",
     @"Unassigned" ?: [NSNull null], @"3SwipeRight",
     @"Unassigned" ?: [NSNull null], @"3SwipeUp",
     @"Unassigned" ?: [NSNull null], @"3SwipeDown",
     @"Unassigned" ?: [NSNull null], @"31Tap",
     @"Unassigned" ?: [NSNull null], @"32Tap",
     @"Unassigned" ?: [NSNull null], @"33Tap",
     @"Unassigned" ?: [NSNull null], @"34Tap",
     @"Unassigned" ?: [NSNull null], @"3LongPress",
     nil];
}

-(void)saveGestureAssignments {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"settings.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    [_assignments writeToFile:fileAndPath atomically:YES];

}

-(void)loadGestures {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"settings.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    _assignments = [[NSMutableDictionary alloc] initWithContentsOfFile:fileAndPath];
    NSLog(@"assignments are %@",_assignments);
}

@end
