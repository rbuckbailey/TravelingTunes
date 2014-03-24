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
    [self loadThemes];
     return self;
}

- (void)initDisplaySettings {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"50" forKey:@"artistFontSize"];
    [defaults setObject:@"70" forKey:@"songFontSize"];
    [defaults setObject:@"50" forKey:@"albumFontSize"];
    [defaults setObject:@"Left" forKey:@"artistAlignment"];
    [defaults setObject:@"Left" forKey:@"songAlignment"];
    [defaults setObject:@"Left" forKey:@"albumAlignment"];
    [defaults synchronize];
}

- (void)initPlaylistSettings {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"1" forKey:@"repeat"];
    [defaults setObject:@"1" forKey:@"shuffle"];
    [defaults setObject:@"play all" forKey:@"playlist"];
    [defaults synchronize];
}

- (void)initThemes {
    //bg, artist, song, album
    self.themes = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255/255.f green: 255/255.f blue:255/255.f    alpha:1],
                                                [UIColor colorWithRed: 170/255.f green: 170/255.f blue:170/255.f    alpha:1],nil],@"greyonwhite",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 0         green: 0           blue:0          alpha:1],
                                                [UIColor colorWithRed: 190/255.f green: 190/255.f   blue:190/255.f  alpha:1],nil],@"greyonblack",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 205/255.f green: 241/255.f   blue:5/255.f    alpha:1],
                                                [UIColor colorWithRed: 98/255.f  green: 128/255.f   blue:29/255.f   alpha:1],nil],@"leaf",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 112/255.f green: 128/255.f   blue:34/255.f   alpha:1],
                                                [UIColor colorWithRed: 179/255.f green: 171/255.f   blue:125/255.f  alpha:1],nil],@"olive",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 155/255.f green: 178/255.f   blue:255/255.f  alpha:1],
                                                [UIColor colorWithRed: 98/255.f  green: 91/255.f    blue:255        alpha:1],nil],@"periwinkleblue",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 195/255.f green: 192/255.f   blue:255/255.f  alpha:1],
                                                [UIColor colorWithRed: 255/255.f green: 255/255.f   blue:255        alpha:1],nil],@"lavender",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255/255.f green: 188/255.f   blue:196/255.f  alpha:1],
                                                [UIColor colorWithRed: 255/255.f green: 239/255.f   blue:242        alpha:1],nil],@"blush",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255/255.f green: 255/255.f   blue:0          alpha:1],
                                                [UIColor colorWithRed: 255/255.f green: 0           blue:0          alpha:1],nil],@"hotdogstand",
                   nil];
    
    [self saveThemes];
}

- (void)initGestureAssignments {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"Previous" forKey:@"1SwipeLeft"];
    [defaults setObject:@"Next" forKey:@"1SwipeRight"];
    [defaults setObject:@"VolumeUp" forKey:@"1SwipeUp"];
    [defaults setObject:@"VolumeDown" forKey:@"1SwipeDown"];
    [defaults setObject:@"PlayPause" forKey:@"11Tap"];
    [defaults setObject:@"Unassigned" forKey:@"12Tap"];
    [defaults setObject:@"Menu" forKey:@"13Tap"];
    [defaults setObject:@"Menu" forKey:@"14Tap"];
    [defaults setObject:@"Menu" forKey:@"1LongPress"];
    [defaults setObject:@"Rewind" forKey:@"2SwipeLeft"];
    [defaults setObject:@"FastForward" forKey:@"2SwipeRight"];
    [defaults setObject:@"Unassigned" forKey:@"2SwipeUp"];
    [defaults setObject:@"Unassigned" forKey:@"2SwipeDown"];
    [defaults setObject:@"Unassigned" forKey:@"21Tap"];
    [defaults setObject:@"Unassigned" forKey:@"22Tap"];
    [defaults setObject:@"Unassigned" forKey:@"23Tap"];
    [defaults setObject:@"Unassigned" forKey:@"24Tap"];
    [defaults setObject:@"Unassigned" forKey:@"2LongPress"];
    [defaults setObject:@"Unassigned" forKey:@"3SwipeLeft"];
    [defaults setObject:@"Unassigned" forKey:@"3SwipeRight"];
    [defaults setObject:@"Unassigned" forKey:@"3SwipeUp"];
    [defaults setObject:@"Unassigned" forKey:@"3SwipeDown"];
    [defaults setObject:@"Unassigned" forKey:@"31Tap"];
    [defaults setObject:@"Unassigned" forKey:@"32Tap"];
    [defaults setObject:@"Unassigned" forKey:@"33Tap"];
    [defaults setObject:@"Unassigned" forKey:@"34Tap"];
    [defaults setObject:@"Unassigned" forKey:@"3LongPress"];
    [defaults synchronize];
}

-(void)saveThemes {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"themes.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    [_themes writeToFile:fileAndPath atomically:YES];
//    NSLog(@"themes are %@",_themes);
}

-(void)loadThemes {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"themes.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    _themes = [[NSMutableDictionary alloc] initWithContentsOfFile:fileAndPath];
    if (_themes==NULL) [self initThemes];
//       NSLog(@"themes are %@",_themes);
}

@end
