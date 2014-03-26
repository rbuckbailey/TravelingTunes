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
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (![defaults objectForKey:@"firstRun"]) {
        [self initGestureAssignments];
        [self initDisplaySettings];
        [self initPlaylistSettings];
        [self initThemes];
        [defaults setObject:@"Yes!" forKey:@"firstRun"]; [defaults synchronize];
    } else [self loadThemes];
/*    [self initGestureAssignments];
    [self initDisplaySettings];
    [self initPlaylistSettings]; */
     return self;
}

- (void)initDisplaySettings {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"70" forKey:@"artistFontSize"];
    [defaults setObject:@"90" forKey:@"songFontSize"];
    [defaults setObject:@"70" forKey:@"albumFontSize"];
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
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (![defaults objectForKey:@"currentTheme"]) [defaults setObject:@"Lavender" forKey:@"currentTheme"];
    [defaults synchronize];
    self.themes = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255/255.f green: 255/255.f blue:255/255.f alpha:1],
                    [UIColor colorWithRed: 170/255.f green: 170/255.f blue:170/255.f alpha:1],nil],@"Grey on White",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 0   green: 0   blue:0   alpha:1],
                    [UIColor colorWithRed: 190/255.f green: 190/255.f blue:190/255.f alpha:1],nil],@"Grey on Black",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 205/255.f   green: 241/255.f   blue:5/255.f   alpha:1],
                    [UIColor colorWithRed: 98/255.f green: 128/255.f blue:29/255.f alpha:1],nil],@"Leaf",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 112/255.f   green: 128/255.f   blue:34/255.f   alpha:1],
                    [UIColor colorWithRed: 179/255.f green: 171/255.f blue:125/255.f alpha:1],nil],@"Olive",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 155/255.f   green: 178/255.f   blue:255/255.f   alpha:1],
                    [UIColor colorWithRed: 98/255.f green: 91/255.f blue:255 alpha:1],nil],@"Periwinkle Blue",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 195/255.f   green: 192/255.f   blue:255/255.f   alpha:1],
                                                [UIColor colorWithRed: 255/255.f green: 255/255.f blue:255 alpha:1],nil],@"Lavender",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255/255.f   green: 188/255.f   blue:196/255.f   alpha:1],
                    [UIColor colorWithRed: 255/255.f green: 239/255.f blue:242 alpha:1],nil],@"Blush",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255/255.f   green: 255/255.f   blue:0   alpha:1],
                    [UIColor colorWithRed: 255/255.f green: 0 blue:0 alpha:1],nil],@"Hot Dog Stand",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 10/255.f   green: 10/255.f   blue:0   alpha:1],
                    [UIColor colorWithRed: 255/255.f green: 0 blue:0 alpha:1],nil],@"Custom",
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
    [defaults setObject:@"Unassigned" forKey:@"13Tap"];
    [defaults setObject:@"Unassigned" forKey:@"14Tap"];
    [defaults setObject:@"Menu" forKey:@"1LongPress"];
    [defaults setObject:@"Rewind" forKey:@"2SwipeLeft"];
    [defaults setObject:@"FastForward" forKey:@"2SwipeRight"];
    [defaults setObject:@"Unassigned" forKey:@"2SwipeUp"];
    [defaults setObject:@"Unassigned" forKey:@"2SwipeDown"];
    [defaults setObject:@"Unassigned" forKey:@"21Tap"];
    [defaults setObject:@"Unassigned" forKey:@"22Tap"];
    [defaults setObject:@"Unassigned" forKey:@"23Tap"];
    [defaults setObject:@"Unassigned" forKey:@"24Tap"];
    [defaults setObject:@"StartDefaultPlaylist" forKey:@"2LongPress"];
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
    NSLog(@"Initializing gestures assignments.");
}

-(void)saveThemes {
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"themes.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];

    NSArray *themecolors;
    NSMutableDictionary *temp = [[NSMutableDictionary alloc] init];

//    NSData *data = [NSKeyedArchiver archivedDataWithRootObject:themecolors];
//    [temp setObject:data forKey:@"Grey on White"];

    themecolors = [_themes objectForKey:@"Grey on White"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Grey on White"];
    themecolors = [_themes objectForKey:@"Grey on Black"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Grey on Black"];
    themecolors = [_themes objectForKey:@"Lavender"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Lavender"];
    themecolors = [_themes objectForKey:@"Leaf"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Leaf"];
    themecolors = [_themes objectForKey:@"Olive"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Olive"];
    themecolors = [_themes objectForKey:@"Blush"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Blush"];
    themecolors = [_themes objectForKey:@"Periwinkle Blue"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Periwinkle Blue"];
    themecolors = [_themes objectForKey:@"Hot Dog Stand"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Hot Dog Stand"];
    themecolors = [_themes objectForKey:@"Custom"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Custom"];

    [temp writeToFile:fileAndPath atomically:YES];
//    NSLog(@"themes are %@",_themes);*/
}

-(void)loadThemes {
    self.themes = [[NSMutableDictionary alloc] init];
    NSArray *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentDirPath = [path objectAtIndex:0];
    NSString *fileName = @"themes.plist";
    NSString *fileAndPath = [documentDirPath stringByAppendingPathComponent:fileName];
    NSMutableDictionary *temp;

    temp = [[NSMutableDictionary alloc] initWithContentsOfFile:fileAndPath];
    
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Grey on White"]] forKey:@"Grey on White"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Grey on Black"]] forKey:@"Grey on Black"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Lavender"]] forKey:@"Lavender"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Leaf"]] forKey:@"Leaf"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Olive"]] forKey:@"Olive"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Blush"]] forKey:@"Blush"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Periwinkle Blue"]] forKey:@"Periwinkle Blue"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Hot Dog Stand"]] forKey:@"Hot Dog Stand"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Custom"]] forKey:@"Custom"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Leaf"]] forKey:@"Leaf"];

    if (_themes==NULL) [self initThemes];
//       NSLog(@"themes are %@",_themes);
}

@end
