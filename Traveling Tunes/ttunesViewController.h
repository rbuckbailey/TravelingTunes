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


@interface ttunesViewController : UIViewController
@property (weak, nonatomic) IBOutlet UILabel *artistTitle;
@property (weak, nonatomic) IBOutlet UILabel *songTitle;
@property (weak, nonatomic) IBOutlet UILabel *albumTitle;
@property NSTimer *timer;

- (IBAction)singleTapDetected:(id)sender;
- (IBAction)longPressDetected:(id)sender;
- (IBAction)pinchDetected:(id)sender;
- (IBAction)rotationDetected:(id)sender;
- (IBAction)panDetected:(id)sender;
- (IBAction)swipeLeftDetected:(id)sender;
- (IBAction)swipeRight:(id)sender;
- (IBAction)swipeUpDetected:(id)sender;
- (IBAction)swipeDownDetected:(id)sender;

@end
