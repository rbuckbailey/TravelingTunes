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
#import "contactsTableViewController.h"
#import <CoreLocation/CoreLocation.h>
#import "LEColorPicker.h"
#import <iAd/iAd.h>
#import <MapKit/MapKit.h>
#import <AVFoundation/AVFoundation.h>

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
BOOL areAdsRemoved;

@interface ttunesViewController : UIViewController <CLLocationManagerDelegate, ADBannerViewDelegate, UIPageViewControllerDataSource, MKMapViewDelegate, AVSpeechSynthesizerDelegate,UITableViewDelegate,UITableViewDataSource>

@property (strong, nonatomic) UIPageViewController *pageController;

//@property (nonatomic, retain) id adBannerView;
//@property (nonatomic) BOOL adBannerViewIsVisible;
@property (nonatomic, assign) BOOL bannerIsVisible;

@property swipeDirections direction;
@property NSTimer *marqueeTimer,*scrollingTimer,*scrubTimer,*fadeHUDTimer,*actionHUDFadeTimer,*DNRTimer,*GPSTimer;
@property MPVolumeView* volume;
@property NSInteger marqueePosition;
@property CLLocationManager *gps;
@property int speedTier,oldSpeedTier;
@property NSArray* playlists;
@property float volumeBase,volumeTenth,volumeTarget;
@property MKMapView *map;
@property CLLocationDirection currentHeading;

- (IBAction)singleTapDetected:(id)sender;
- (IBAction)longPressDetected:(id)sender;
- (void)performPlayerAction:(NSString *)action :(NSString*)sender;
- (void)setupDestinationAddress;


- (void)setupLabels;

@end
