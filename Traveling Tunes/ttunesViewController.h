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
#import <CoreLocation/CoreLocation.h>
#import "LEColorPicker.h"
#import <iAd/iAd.h>

CGFloat const gestureMinimumTranslation = 20.0;

typedef enum : NSInteger {
    directionNone,
    directionUp,
    directionDown,
    directionRight,
    directionLeft
} swipeDirections;

ADBannerView *adBanner;
BOOL bannerIsVisible;

@interface ttunesViewController : UIViewController <CLLocationManagerDelegate, ADBannerViewDelegate>

//@property (nonatomic, retain) id adBannerView;
//@property (nonatomic) BOOL adBannerViewIsVisible;
@property (nonatomic, retain) IBOutlet ADBannerView *adBanner;
@property (nonatomic, assign) BOOL bannerIsVisible;

@property swipeDirections direction;
@property (weak, nonatomic) IBOutlet UILabel *artistTitle;
@property (weak, nonatomic) IBOutlet UILabel *songTitle;
@property (weak, nonatomic) IBOutlet UILabel *albumTitle;
@property NSTimer *marqueeTimer,*scrollingTimer,*scrubTimer,*fadeHUDTimer,*actionHUDFadeTimer,*DNRTimer;
@property MPVolumeView* volume;
@property NSInteger marqueePosition;
@property CLLocationManager *gps;
@property int speedTier,oldSpeedTier;
@property NSArray* playlists;
@property float volumeBase,volumeTenth,volumeTarget;

@property (weak, nonatomic) IBOutlet UILabel *gpsTest;


- (IBAction)singleTapDetected:(id)sender;
- (IBAction)longPressDetected:(id)sender;
- (void)performPlayerAction:(NSString *)action :(NSString*)sender;


- (void)setupLabels;

@end
