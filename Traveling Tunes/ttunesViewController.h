//
//  ttunesViewController.h
//  Traveling Tunes
//
//  Created by buck on 1/31/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <MediaPlayer/MediaPlayer.h>



@interface ttunesViewController : UIViewController
@property (strong, nonatomic) IBOutlet UILabel *songTitle;
//- (void)selectActionFromString:(int)action :(NSString*)sender;
- (IBAction)singleTapDetected:(id)sender;
//- (IBAction)longPressDetected:(id)sender;
- (IBAction)pinchDetected:(id)sender;
- (IBAction)rotationDetected:(id)sender;
- (IBAction)panDetected:(id)sender;
- (IBAction)swipeLeftDetected:(id)sender;
- (IBAction)swipeRight:(id)sender;
- (IBAction)swipeUpDetected:(id)sender;
- (IBAction)swipeDownDetected:(id)sender;

@end
