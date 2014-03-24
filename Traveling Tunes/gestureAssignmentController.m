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
    [self loadGestures];
    [self loadDisplaySettings];
    [self loadPlaylistSettings];
    [self loadThemes];
     return self;
}

- (void)initDisplaySettings {
    self.displaySettings = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                            @"50",@"artistFontSize",
                            @"60",@"songFontSize",
                            @"50",@"albumFontSize",
                            @"Left",@"artistAlignment",
                            @"Left",@"songAlignment",
                            @"Left",@"albumAlignment",
                            nil];
    [self saveDisplaySettings];
}

- (void)initPlaylistSettings {
    self.playlistSettings = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                             @"1",@"repeat",
                             @"1",@"shuffle",
                             @"play all",@"playlist",
                             nil];
    [self savePlaylistSettings];
}

- (void)initThemes {
//    NSArray *temp = [NSArray arrayWithObjects:[UIColor colorWithRed: 25 green: 25 blue:25 alpha:1],[UIColor colorWithRed: 25 green: 25 blue:25 alpha:1],[UIColor colorWithRed: 25 green: 25 blue:25 alpha:1],nil];
    
    //bg, artist, song, album
    self.themes = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255 green: 255 blue:255 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],
                                                [UIColor colorWithRed: 130 green: 130 blue:130 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],nil],@"greyOnWhite",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 0   green: 0   blue:0   alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:0.8],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],nil],@"alphaTest",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 0   green: 0   blue:0   alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],
                                                [UIColor colorWithRed: 190 green: 190 blue:190 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],nil],@"greyOnBlack",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 205   green: 241   blue:5   alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],
                                                [UIColor colorWithRed: 98 green: 128 blue:29 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],nil],@"leaf",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 112   green: 128   blue:34   alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],
                                                [UIColor colorWithRed: 179 green: 171 blue:125 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],nil],@"olive",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 155   green: 178   blue:255   alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],
                                                [UIColor colorWithRed: 98 green: 91 blue:255 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],nil],@"periwinkleblue",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 195   green: 192   blue:255   alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],
                                                [UIColor colorWithRed: 255 green: 255 blue:255 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],nil],@"lavender",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255   green: 188   blue:196   alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],
                                                [UIColor colorWithRed: 255 green: 239 blue:242 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],nil],@"blush",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255   green: 0   blue:0   alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],
                                                [UIColor colorWithRed: 255 green: 255 blue:0 alpha:1],
                                                [UIColor colorWithRed: 170 green: 170 blue:170 alpha:1],nil],@"hotdogstand",
                   nil];
    
    [self saveThemes];
}

- (void)initGestureAssignments {
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
    [self saveGestureAssignments];
}

-(void)saveGestureAssignments {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"gestures.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    [_assignments writeToFile:fileAndPath atomically:YES];
    //    NSLog(@"assignments are %@",_assignments);
}

-(void)loadGestures {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"settings.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    _assignments = [[NSMutableDictionary alloc] initWithContentsOfFile:fileAndPath];
    if (_assignments==NULL) [self initGestureAssignments];
    //    NSLog(@"assignments are %@",_assignments);
}

-(void)saveThemes {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"themes.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    [_assignments writeToFile:fileAndPath atomically:YES];
        NSLog(@"themes are %@",_themes);
}

-(void)loadThemes {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"themes.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    _assignments = [[NSMutableDictionary alloc] initWithContentsOfFile:fileAndPath];
    if (_assignments==NULL) [self initThemes];
    //    NSLog(@"themes are %@",_assignments);
}

-(void)saveDisplaySettings {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"display.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    [_displaySettings writeToFile:fileAndPath atomically:YES];
//    NSLog(@"display settings are %@",_displaySettings);
    
}

-(void)loadDisplaySettings {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"display.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    _displaySettings = [[NSMutableDictionary alloc] initWithContentsOfFile:fileAndPath];
    if (_displaySettings==NULL) [self initDisplaySettings];
//    NSLog(@"display settings are %@",_displaySettings);
}

-(void)savePlaylistSettings {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"playlist.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    [_playlistSettings writeToFile:fileAndPath atomically:YES];
//    NSLog(@"playlist settings are %@",_playlistSettings);
    
}

-(void)loadPlaylistSettings {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"playlist.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    _playlistSettings = [[NSMutableDictionary alloc] initWithContentsOfFile:fileAndPath];
    if (_playlistSettings==NULL) [self initPlaylistSettings];
//    NSLog(@"playlist settings are %@",_playlistSettings);
}

@end
