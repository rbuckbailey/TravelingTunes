//
//  ttunesViewController.h
//  Traveling Tunes
//
//  Created by buck on 1/31/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <MediaPlayer/MediaPlayer.h>
#import "gestureAssignmentController.h"

CGFloat const gestureMinimumTranslation = 20.0;

typedef enum : NSInteger {
    directionNone,
    directionUp,
    directionDown,
    directionRight,
    directionLeft
} swipeDirections;

@interface ttunesViewController : UIViewController
@property swipeDirections direction;
@property (weak, nonatomic) IBOutlet UILabel *artistTitle;
@property (weak, nonatomic) IBOutlet UILabel *songTitle;
@property (weak, nonatomic) IBOutlet UILabel *albumTitle;
@property NSTimer *timer;
@property MPVolumeView* volume;
@property NSInteger marqueePosition;



- (IBAction)singleTapDetected:(id)sender;
- (IBAction)longPressDetected:(id)sender;

- (void)setupLabels;

@end
