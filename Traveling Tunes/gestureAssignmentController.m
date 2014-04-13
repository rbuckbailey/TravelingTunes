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
        [self initAllSettings];
        [defaults setObject:@"QS" forKey:@"firstRun"]; [defaults synchronize];
    } else [self loadThemes];
     return self;
}

- (void) initAllSettings {
    [self initGestureAssignments];
    [self initDisplaySettings];
    [self initPlaylistSettings];
    [self initThemes];
    [self initOtherSettings];
}

- (void)initOtherSettings {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"0.01" forKey:@"volumeSensitivity"];
    [defaults setObject:@"5" forKey:@"seekSensitivity"];
    [defaults setObject:@"NO" forKey:@"ShowStatusBar"];
    [defaults setObject:@"YES" forKey:@"VolumeAlwaysOn"];
    [defaults setObject:@"1" forKey:@"HUDType"];
    [defaults setObject:@"2" forKey:@"ScrubHUDType"];
    [defaults setObject:@"YES" forKey:@"RotationClockwise"];
    [defaults setObject:@"YES" forKey:@"RotationAntiClockwise"];
    [defaults setObject:@"YES" forKey:@"RotationInverted"];
    [defaults setObject:@"YES" forKey:@"RotationPortrait"];
    [defaults setObject:@"NO" forKey:@"PlayOnLaunch"];
    [defaults setObject:@"NO" forKey:@"PauseOnExit"];
    [defaults setObject:@"YES" forKey:@"titleShrinkInPortrait"];
    [defaults setObject:@"YES" forKey:@"titleShrinkLong"];
    [defaults setObject:@"NO" forKey:@"InvertAtNight"];
    [defaults setObject:@"YES" forKey:@"DimAtNight"];
    [defaults setObject:@"6" forKey:@"SunRiseHour"];
    [defaults setObject:@"19" forKey:@"SunSetHour"];
    [defaults setObject:@"NO" forKey:@"TitleScrollLong"];
    [defaults setObject:@"NO" forKey:@"GPSVolume"];
    [defaults setObject:@"0.5" forKey:@"GPSSensivity"];
    [defaults setObject:@"YES" forKey:@"showAlbumArt"];
    [defaults setObject:@"YES" forKey:@"albumArtColors"];
    [defaults setObject:@"YES" forKey:@"showActions"];
    [defaults setObject:@"NO" forKey:@"disableAdBanners"];
}

- (void)initDisplaySettings {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"50" forKey:@"artistFontSize"];
    [defaults setObject:@"70" forKey:@"songFontSize"];
    [defaults setObject:@"55" forKey:@"albumFontSize"];
    [defaults setObject:@"Left" forKey:@"artistAlignment"];
    [defaults setObject:@"Left" forKey:@"songAlignment"];
    [defaults setObject:@"Left" forKey:@"albumAlignment"];
    [defaults setObject:@"35" forKey:@"minimumFontSize"];
    [defaults synchronize];
}

- (void)initPlaylistSettings {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"YES" forKey:@"repeat"];
    [defaults setObject:@"YES" forKey:@"shuffle"];
    [defaults setObject:@"All Songs, Shuffled" forKey:@"playlist"];
    [defaults synchronize];
}

- (void)initThemes {
    //bg, artist, song, album
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"Lavender" forKey:@"currentTheme"];
    
    [defaults synchronize];
    self.themes = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 170/255.f green: 170/255.f blue:170/255.f alpha:1],
                                                [UIColor colorWithRed: 255/255.f green: 255/255.f blue:255/255.f alpha:1],nil],@"White on Grey",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 0   green: 0   blue:0   alpha:1],
                                                [UIColor colorWithRed: 190/255.f green: 190/255.f blue:190/255.f alpha:1],nil],@"Grey on Black",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 205/255.f   green: 241/255.f   blue:5/255.f   alpha:1],
                                                [UIColor colorWithRed: 98/255.f green: 128/255.f blue:29/255.f alpha:1],nil],@"Leaf",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 241/255.f   green: 195/255.f   blue:146/255.f   alpha:1],
                                                [UIColor colorWithRed: 119/255.f green: 68/255.f blue:39/255.f alpha:1],nil],@"Old West",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 98/255.f green: 91/255.f blue:255 alpha:1],
                                                [UIColor colorWithRed: 155/255.f   green: 178/255.f   blue:255/255.f   alpha:1],nil],@"Periwinkle Blue",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 195/255.f   green: 192/255.f   blue:255/255.f   alpha:1],
                                                [UIColor colorWithRed: 255/255.f green: 255/255.f blue:255 alpha:1],nil],@"Lavender",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255/255.f   green: 188/255.f   blue:196/255.f   alpha:1],
                                                [UIColor colorWithRed: 255/255.f green: 239/255.f blue:242 alpha:1],nil],@"Blush",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 255/255.f   green: 255/255.f   blue:0   alpha:1],
                                                [UIColor colorWithRed: 255/255.f green: 0 blue:0 alpha:1],nil],@"Hot Dog Stand",
                   [NSArray arrayWithObjects:   [UIColor colorWithRed: 10/255.f   green: 10/255.f   blue:10/255.f   alpha:1],
                                                [UIColor colorWithRed: 255/255.f green: 255/255.f blue:255/255.f alpha:1],nil],@"Custom",
                   nil];
    [defaults setObject:[NSNumber numberWithFloat:22] forKey:@"customTextRed"];
    [defaults setObject:[NSNumber numberWithFloat:22] forKey:@"customTextGreen"];
    [defaults setObject:[NSNumber numberWithFloat:180] forKey:@"customTextBlue"];
    [defaults setObject:[NSNumber numberWithFloat:200] forKey:@"customBGRed"];
    [defaults setObject:[NSNumber numberWithFloat:200] forKey:@"customBGGreen"];
    [defaults setObject:[NSNumber numberWithFloat:100] forKey:@"customBGBlue"];
    [_themes setObject:[NSArray arrayWithObjects:
                                           [UIColor colorWithRed: (int)[[defaults objectForKey:@"customBGRed"] floatValue]/255.f
                                                           green: (int)[[defaults objectForKey:@"customBGGreen"] floatValue]/255.f
                                                            blue: (int)[[defaults objectForKey:@"customBGBlue"] floatValue]/255.f   alpha:1],
                                           [UIColor colorWithRed: (int)[[defaults objectForKey:@"customTextRed"] floatValue]/255.f
                                                           green: (int)[[defaults objectForKey:@"customTextGreen"] floatValue]/255.f
                                                            blue: (int)[[defaults objectForKey:@"customTextBlue"] floatValue]/255.f alpha:1],nil] forKey:@"Custom"];
    

    [self saveThemes];
}

- (void)initGestureAssignments {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"Rewind" forKey:@"1SwipeLeft"];
    [defaults setObject:@"FastForward" forKey:@"1SwipeRight"];
//    [defaults setObject:@"RestartPrevious" forKey:@"1SwipeLeft"];
//    [defaults setObject:@"Next" forKey:@"1SwipeRight"];
    [defaults setObject:@"VolumeUp" forKey:@"1SwipeUp"];
    [defaults setObject:@"VolumeDown" forKey:@"1SwipeDown"];
    [defaults setObject:@"YES" forKey:@"1SwipeLeftContinuous"];
    [defaults setObject:@"YES" forKey:@"1SwipeRightContinuous"];
    [defaults setObject:@"YES" forKey:@"1SwipeUpContinuous"];
    [defaults setObject:@"YES" forKey:@"1SwipeDownContinuous"];
    [defaults setObject:@"PlayPause" forKey:@"11Tap"];
    [defaults setObject:@"Next" forKey:@"12Tap"];
    [defaults setObject:@"Previous" forKey:@"13Tap"];
    [defaults setObject:@"Unassigned" forKey:@"14Tap"];
    [defaults setObject:@"Menu" forKey:@"1LongPress"];

    [defaults setObject:@"RestartPrevious" forKey:@"2SwipeLeft"];
    [defaults setObject:@"Next" forKey:@"2SwipeRight"];
    [defaults setObject:@"Unassigned" forKey:@"2SwipeUp"];
    [defaults setObject:@"Unassigned" forKey:@"2SwipeDown"];
    [defaults setObject:@"NO" forKey:@"2SwipeLeftContinuous"];
    [defaults setObject:@"NO" forKey:@"2SwipeRightContinuous"];
    [defaults setObject:@"NO" forKey:@"2SwipeUpContinuous"];
    [defaults setObject:@"NO" forKey:@"2SwipeDownContinuous"];
    [defaults setObject:@"SongPicker" forKey:@"21Tap"];
    [defaults setObject:@"Next" forKey:@"22Tap"];
    [defaults setObject:@"Previous" forKey:@"23Tap"];
    [defaults setObject:@"Unassigned" forKey:@"24Tap"];
    [defaults setObject:@"Unassigned" forKey:@"2LongPress"];

    [defaults setObject:@"Unassigned" forKey:@"3SwipeLeft"];
    [defaults setObject:@"Unassigned" forKey:@"3SwipeRight"];
    [defaults setObject:@"PlayCurrentArtist" forKey:@"3SwipeUp"];
    [defaults setObject:@"PlayCurrentAlbum" forKey:@"3SwipeDown"];
    [defaults setObject:@"NO" forKey:@"3SwipeLeftContinuous"];
    [defaults setObject:@"NO" forKey:@"3SwipeRightContinuous"];
    [defaults setObject:@"NO" forKey:@"3SwipeUpContinuous"];
    [defaults setObject:@"NO" forKey:@"3SwipeDownContinuous"];
    [defaults setObject:@"Unassigned" forKey:@"31Tap"];
    [defaults setObject:@"Unassigned" forKey:@"32Tap"];
    [defaults setObject:@"Unassigned" forKey:@"33Tap"];
    [defaults setObject:@"Unassigned" forKey:@"34Tap"];
    [defaults setObject:@"StartDefaultPlaylist" forKey:@"3LongPress"];
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
//    [temp setObject:data forKey:@"White on Grey"];

    themecolors = [_themes objectForKey:@"White on Grey"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"White on Grey"];
    themecolors = [_themes objectForKey:@"Grey on Black"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Grey on Black"];
    themecolors = [_themes objectForKey:@"Lavender"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Lavender"];
    themecolors = [_themes objectForKey:@"Leaf"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Leaf"];
    themecolors = [_themes objectForKey:@"Old West"];
    [temp setObject:[NSKeyedArchiver archivedDataWithRootObject:themecolors] forKey:@"Old West"];
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
    
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"White on Grey"]] forKey:@"White on Grey"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Grey on Black"]] forKey:@"Grey on Black"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Lavender"]] forKey:@"Lavender"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Leaf"]] forKey:@"Leaf"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Old West"]] forKey:@"Old West"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Blush"]] forKey:@"Blush"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Periwinkle Blue"]] forKey:@"Periwinkle Blue"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Hot Dog Stand"]] forKey:@"Hot Dog Stand"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Custom"]] forKey:@"Custom"];
    [_themes setObject:[NSKeyedUnarchiver unarchiveObjectWithData:[temp objectForKey:@"Leaf"]] forKey:@"Leaf"];

    if (_themes==NULL) [self initThemes];
//       NSLog(@"themes are %@",_themes);
}

@end
