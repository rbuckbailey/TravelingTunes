//
//  ttunesViewController.m
//  Traveling Tunes
//
//  Created by buck on 1/31/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "ttunesViewController.h"
#import "gestureAssignmentController.h"
#import "settingsTableViewController.h"
#import "ttunesAppDelegate.h"




MPMusicPlayerController*        mediaPlayer;

int leftMargin = 20;
int rightMargin = 20;
int topMargin = 20;
int bottomMargin = 20;
int albumTitleY = 0;
int songTitleY = 0;
int artistPosition = 0;
int songPosition = 0;
int albumPosition = 0;
BOOL topButtons = NO;
BOOL bottomButtons = NO;

@interface ttunesViewController ()
@property UIView *lineView,*playbackLineView,*edgeViewBG,*playbackEdgeViewBG,*nightTimeFade,*bgView;
@property UILabel *actionHUD;
@property UILabel *topLeftRegion,*topCenterRegion,*topRightRegion,*bottomLeftRegion,*bottomCenterRegion,*bottomRightRegion;
@property UILabel *artistTitle,*songTitle,*albumTitle;
@property UILabel *gpsDistanceRemaining;
@property int timersRunning;
@property float adjustedSongFontSize,fadeHUDalpha,fadeActionHUDAlpha;
@property int activeOrientation;
@property (strong, nonatomic) CLLocationManager *locationManager;
@property int baseVolume;
@property int fingers;
@property UIColor *themeBG, *themeColorArtist,*themeColorSong,*themeColorAlbum;
@property UIImageView *albumArt;

@end


@implementation ttunesViewController

CLPlacemark *thePlacemark;
MKRoute *routeDetails;


/*** initialization and watchers ********************************************************************************************************************************************/


- (BOOL) shouldAutorotate {
    return TRUE;
}

- (BOOL)prefersStatusBarHidden {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ([[defaults objectForKey:@"ShowStatusBar"] isEqual:@"NO"]) return YES;
    else return NO;
}

-(UIStatusBarStyle)preferredStatusBarStyle{
    return UIStatusBarStyleLightContent;
}


- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerBeforeViewController:(UIViewController *)viewController {
    NSUInteger index = [(quickstartViewController *)viewController index];
    if (index == 0) {
        index = 4;
    }
    index--;
    return [self viewControllerAtIndex:index];
}

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerAfterViewController:(UIViewController *)viewController {
    NSUInteger index = [(quickstartViewController *)viewController index];
    index++;
    if (index == 4) {
        index=0;
    }
    return [self viewControllerAtIndex:index];
}

- (quickstartViewController *)viewControllerAtIndex:(NSUInteger)index {
    quickstartViewController *childViewController = [[quickstartViewController alloc] init ];    childViewController.index = index;
    return childViewController;
}

- (NSInteger)presentationCountForPageViewController:(UIPageViewController *)pageViewController {
    // The number of items reflected in the page indicator.
    return 4;
}

- (NSInteger)presentationIndexForPageViewController:(UIPageViewController *)pageViewController {
    // The selected item reflected in the page indicator.
    return 0;
}

- (void) showInstructions {
    [self scrubTimerKiller];
    [self.navigationController setNavigationBarHidden:YES];
    
    self.pageController = [[UIPageViewController alloc] initWithTransitionStyle:UIPageViewControllerTransitionStyleScroll navigationOrientation:UIPageViewControllerNavigationOrientationHorizontal options:nil];
    
    self.pageController.dataSource = self;
    [[self.pageController view] setFrame:[[self view] bounds]];
    
    quickstartViewController *initialViewController = [self viewControllerAtIndex:0];
    
    NSArray *viewControllers = [NSArray arrayWithObject:initialViewController];
    [self.pageController setViewControllers:viewControllers direction:UIPageViewControllerNavigationDirectionForward animated:NO completion:nil];
    [self.navigationController pushViewController:self.pageController animated:YES];
}


- (void)deviceOrientationDidChangeNotification:(NSNotification*)note
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    UIDeviceOrientation orientation = [[UIDevice currentDevice] orientation];

    // do not reset marquee if the device has been set face-up (orientation 5), but do reset for other changes
    if (orientation != 5) {
        if (_activeOrientation != orientation) {
            [self scrollingTimerKiller]; [self startMarqueeTimer];
        }
        _activeOrientation = orientation;
    }
    //clear actionHUD if action
    _actionHUD.backgroundColor = [UIColor clearColor];
    _actionHUD.textColor = [UIColor clearColor];
    [self.actionHUDFadeTimer invalidate];
    
    [self setupLabels];
    [self setupHUD];

    if (self.bannerIsVisible) adBanner.frame = CGRectMake(0,self.view.bounds.size.height-[self getBannerHeight],self.view.bounds.size.width,[self getBannerHeight]);
    else adBanner.frame = CGRectMake(0,self.view.bounds.size.height,self.view.bounds.size.width,[self getBannerHeight]);
}

- (IBAction)singleTapDetected:(id)sender {
/*    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"11Tap"]:@"11Tap"];
    NSLog(@"gesture is %@",[defaults objectForKey:@"11Tap"]);
 */
}

-(void)startGPSVolume {
    if (self != nil) {
        self.gps = [[CLLocationManager alloc] init];
        self.gps.delegate = self;
        [self.gps startUpdatingLocation];
    }
}

- (void)startGPSHeading {
    // Start heading updates.
//    if ([CLLocationManager headingAvailable]) {
        self.gps.desiredAccuracy = kCLLocationAccuracyBest;

        self.gps.headingFilter = 5;
        [self.gps startUpdatingHeading];
 //   }
}

- (id)init{
//    mediaPlayer = [MPMusicPlayerCscale buttontroller iPodMusicPlayer];
    self = [super init];
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ([[defaults objectForKey:@"GPSVolume"] isEqual:@"YES"]) [self startGPSVolume];
    return self;
}

- (void)setupBaseVolume {
//
}

- (void)locationManager:(CLLocationManager *)manager didUpdateToLocation:(CLLocation *)newLocation fromLocation:(CLLocation *)oldLocation
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    float mph = (newLocation.speed*2.23694);

    // calibrate base volume when not moving; otherwise it is adjusted by [self increase/decreaseVolume]
    if (mph <= 0) {
        _volumeBase = mediaPlayer.volume;
        _volumeTenth = _volumeBase/100;
    }
    
    _speedTier = (int)(mph / 1);
    _volumeTarget = _volumeBase+((_volumeTenth*_speedTier)*[[defaults objectForKey:@"GPSSensivity"] floatValue]);

//    [self setupHUD];
    
/*    NSLog(@"*** gps moved ***");
    NSLog(@"base volume:%f",_volumeBase);
    NSLog(@"real volume:%f",mediaPlayer.volume);
    NSLog(@"target volume:%f",_volumeTarget);
    NSLog(@"Speed %f is %f mph", newLocation.speed,newLocation.speed*2.23694);
*/
}

- (int)getBannerHeight:(UIDeviceOrientation)orientation {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ((!self.bannerIsVisible)|[[defaults objectForKey:@"disableAdBanners" ]  isEqual:@"YES"]) return 0;
    if (orientation==5) {
        if (UIInterfaceOrientationIsLandscape(_activeOrientation)) {
            return 32;
        } else if (UIInterfaceOrientationIsPortrait(_activeOrientation)) {
            return 50;
        }
    } else if (UIInterfaceOrientationIsLandscape(orientation)) {
            return 32;
        } else if (UIInterfaceOrientationIsPortrait(orientation)) {
            return 50;
        }
        return 50;
}

- (int)getBannerHeight {
    return [self getBannerHeight:[UIDevice currentDevice].orientation];
}

#pragma mark ADBannerViewDelegate

- (void) killAdBanner {
    if (adBanner!=NULL) { [adBanner removeFromSuperview]; adBanner=NULL; }
}

- (BOOL)bannerViewActionShouldBegin:(ADBannerView *)banner willLeaveApplication:(BOOL)willLeave
{
    NSLog(@"Banner view is beginning an ad action");
    /*BOOL shouldExecuteAction = [self allowActionToRun]; // your app implements this method
    if (!willLeave && shouldExecuteAction)
    {
        // insert code here to suspend any services that might conflict with the advertisement
    }
    return shouldExecuteAction;*/
    [self scrubTimerKiller];
    return YES;
}

- (void) bannerViewActionDidFinish:(ADBannerView *)banner
{
    if (self.bannerIsVisible) adBanner.frame = CGRectMake(0,self.view.bounds.size.height-[self getBannerHeight],self.view.bounds.size.width,[self getBannerHeight]);
    else adBanner.frame = CGRectMake(0,self.view.bounds.size.height,self.view.bounds.size.width,[self getBannerHeight]);
}

-(void)bannerViewDidLoadAd:(ADBannerView *)banner
{
    if (!self.bannerIsVisible) {
        [UIView beginAnimations:@"animateAdBannerOn" context:NULL]; banner.frame = CGRectOffset(banner.frame, 0, -banner.frame.size.height);
//                                                                    _songTitle.frame = CGRectOffset(_songTitle.frame, 0, -(banner.frame.size.height/2));
//                                                                    _albumTitle.frame = CGRectOffset(_albumTitle.frame, 0, -banner.frame.size.height);
        [UIView commitAnimations]; self.bannerIsVisible = YES; }
    //        if (self.bannerIsVisible) _albumTitle.frame = CGRectOffset(_albumTitle.frame, 0, -_albumTitle.frame.size.height);

}

//Hide banner if can't load ad.
-(void)bannerView:(ADBannerView *)banner didFailToReceiveAdWithError:(NSError *)error
{
    if (self.bannerIsVisible) {
        [UIView beginAnimations:@"animateAdBannerOff" context:NULL]; banner.frame = CGRectOffset(banner.frame, 0, +banner.frame.size.height);
//                                                                    _songTitle.frame = CGRectOffset(_songTitle.frame, 0, +(banner.frame.size.height/2));
//                                                                    _albumTitle.frame = CGRectOffset(_albumTitle.frame, 0, +banner.frame.size.height);
        [UIView commitAnimations]; self.bannerIsVisible = NO; }
    NSLog(@"Ad loading error");
}

- (void)viewWillAppear:(BOOL)animated
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    _volumeTarget = mediaPlayer.volume;

//    if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"2"]) bottomMargin = 40; else bottomMargin = 20;
    
    if ([[defaults objectForKey:@"GPSVolume"] isEqual:@"YES"]) [self startGPSVolume];
    
    [self.navigationController setNavigationBarHidden:YES];
    
#ifdef FREE
    if ([[defaults objectForKey:@"disableAdBanners"] isEqual:@"YES"]) [self killAdBanner];
    if ([[defaults objectForKey:@"disableAdBanners"] isEqual:@"NO"]) [self initAdBanner];
    
    NSLog(@"ads are disabled? %@",[defaults objectForKey:@"disableAdBanners"]);
#endif
    
    if ([[defaults objectForKey:@"showAlbumArt"] isEqual:@"NO"]) _albumArt.alpha = 0.0f;
    if ([[defaults objectForKey:@"shuffle"] isEqual:@"YES"]) mediaPlayer.shuffleMode = MPMusicShuffleModeSongs; else mediaPlayer.shuffleMode = MPMusicShuffleModeOff;
    if ([[defaults objectForKey:@"repeat"] isEqual:@"YES"]) mediaPlayer.repeatMode = MPMusicRepeatModeAll; else mediaPlayer.repeatMode = MPMusicRepeatModeNone;
    
    [self startPlaybackWatcher];
    [self setupLabels];
    [self setupHUD];
    [self setupSystemHUD];
    
    //reset marquee
    [self scrollingTimerKiller];
    [self firstStartTimer];
    
    //start gps if enabled
    if ([[defaults objectForKey:@"GPSVolume"] isEqual:@"YES"])[self.gps startUpdatingLocation];
    else [self.gps stopUpdatingLocation];
    
    // initialize map view
    [self initMapView];

    //disable sleep mode
    if ([[defaults objectForKey:@"DisableAutoLock"] isEqual:@"YES"]) { [[UIApplication sharedApplication] setIdleTimerDisabled:YES]; NSLog(@"sleep off"); }
    else { [[UIApplication sharedApplication] setIdleTimerDisabled:NO]; NSLog(@"sleep on"); }
    
    //if there's an address to navigate to, do that
    if (![[defaults objectForKey:@"destinationAddress"] isEqual:@"narf!"]) {
        [self addressSearch:[defaults objectForKey:@"destinationAddress"]];
        [defaults setObject:@"narf!" forKey:@"destinationAddress"];
    }
}

- (void)locationManager:(CLLocationManager *)manager didUpdateHeading:(CLHeading *)newHeading {
    if (newHeading.headingAccuracy < 0)
        return;
    /*
    // Use the true heading if it is valid.
    CLLocationDirection  theHeading = ((newHeading.trueHeading > 0) ?
                                       newHeading.trueHeading : newHeading.magneticHeading);
    _currentHeading = theHeading;
//    [_map setTransform:CGAffineTransformMakeRotation(-1*newHeading.magneticHeading*3.14159/180)];*/
//    float rotation = -1.0f * M_PI * (newHeading.magneticHeading) / 180.0f; // or .trueHeading for GPS
//	_map.transform = CGAffineTransformMakeRotation(rotation);
//    [_map setUserTrackingMode:MKUserTrackingModeFollowWithHeading animated:NO];
}

- (void)mapView:(MKMapView *)mapView didUpdateUserLocation: (MKUserLocation *)userLocation
{
//    [_map.camera setAltitude:1400+(_speedTier*10)];
    _gpsDistanceRemaining.text = [NSString stringWithFormat:@"%0.1f Miles", routeDetails.distance/1609.344];
//    NSLog(@"distance %@",[NSString stringWithFormat:@"%0.1f Miles", routeDetails.distance/1609.344]);
//    [_map.camera setAltitude:400+(_speedTier*10)];
//    [_map setCenterCoordinate:_map.userLocation.coordinate animated:NO];
//    [_map setUserTrackingMode:MKUserTrackingModeFollowWithHeading animated:NO];
}

- (void) setMapInteractivity {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSLog(@"art style %@",[defaults objectForKey:@"artDisplayStyle"]);
    if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"0"]) {
        _map.zoomEnabled = NO;
        _map.scrollEnabled = NO;
        _map.userInteractionEnabled = NO;
    } else {
        _map.zoomEnabled = YES;
        _map.scrollEnabled = YES;
        _map.userInteractionEnabled = YES;
    }
}

- (void)initMapView{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    if ([[defaults objectForKey:@"showMap"] isEqual:@"YES"]) {
        if (_map==NULL) {
            _map = [[MKMapView alloc] initWithFrame: self.view.bounds];
            _map.delegate = self;
            [self.view addSubview:_map];
//            [self bringTitlesToFront];
            [self bringHUDSToFront];
            _map.showsUserLocation=YES;
            [_map.camera setAltitude:1400+(_speedTier*10)];

            //disable map interaction in overlay mode
            [_map setUserTrackingMode:MKUserTrackingModeFollowWithHeading animated:NO];
            [self startGPSHeading];
        }
        [self setMapInteractivity];
        // max opacity of map if there is art
        MPMediaItemArtwork *artwork = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtwork];
        if ([[defaults objectForKey:@"showMap"] isEqual:@"YES"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]&[[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]) {
            // if both are on display,  left-side map should be at full alpha
            [_map setAlpha:1];
            [self.view bringSubviewToFront:_gpsDistanceRemaining];
        }
        else if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]) [_map setAlpha:1];
        else {
            float mapFade = [[defaults objectForKey:@"AlbumArtFade"] floatValue]*3; //*3; //fade map less than art b/c we want to see it
            if (mapFade>0.75) mapFade = 0.75;  //never completely fade out text
            [_map setAlpha:mapFade];
        }
    } else { [_map removeFromSuperview]; _map=NULL; }
}

- (void) bringTitlesToFront {
    [self.view bringSubviewToFront:_edgeViewBG];
    [self.view bringSubviewToFront:_playbackEdgeViewBG];
    [self.view bringSubviewToFront:_lineView];
    [self.view bringSubviewToFront:_playbackLineView];
    [self.view bringSubviewToFront:_topLeftRegion];
    [self.view bringSubviewToFront:_topCenterRegion];
    [self.view bringSubviewToFront:_topRightRegion];
    [self.view bringSubviewToFront:_bottomLeftRegion];
    [self.view bringSubviewToFront:_bottomCenterRegion];
    [self.view bringSubviewToFront:_bottomRightRegion];
    
    [self.view bringSubviewToFront:_artistTitle];
    [self.view bringSubviewToFront:_songTitle];
    [self.view bringSubviewToFront:_albumTitle];
}

- (void) bringHUDSToFront {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self.view bringSubviewToFront:_actionHUD];
//    if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"0"]) [self.view bringSubviewToFront:_nightTimeFade];
}

- (void)viewDidLoad
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [super viewDidLoad];
    //initialized and not used for a good reason that I don't rememeber now
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];

    _volumeTarget = mediaPlayer.volume;
    _timersRunning=0;
    
    // attach to delegate so launch/exit actions can be called
    ttunesAppDelegate *appDelegate = [[UIApplication sharedApplication] delegate];
    appDelegate.ttunes = self;

    _lineView = [[UIView alloc] init];
    _edgeViewBG = [[UIView alloc] init];
    _playbackEdgeViewBG = [[UIView alloc] init];
    _playbackLineView = [[UIView alloc] init];
    _nightTimeFade = [[UIView alloc] init];
    _actionHUD = [[UILabel alloc] init];
    _albumArt = [[UIImageView alloc] init];
    _bgView = [[UIImageView alloc] init];
    
    _artistTitle = [[UILabel alloc] init];
    _songTitle = [[UILabel alloc] init];
    _albumTitle = [[UILabel alloc] init];

    _topLeftRegion = [[UILabel alloc] init];
    _topCenterRegion = [[UILabel alloc] init];
    _topRightRegion = [[UILabel alloc] init];
    _bottomLeftRegion = [[UILabel alloc] init];
    _bottomCenterRegion = [[UILabel alloc] init];
    _bottomRightRegion = [[UILabel alloc] init];
    
    _gpsDistanceRemaining = [[UILabel alloc] init];
    
    [self.view addSubview:_bgView];
    [self.view addSubview:_albumArt];
    [self.view bringSubviewToFront:_map];
    
    [self.view addSubview:_edgeViewBG];
    [self.view addSubview:_playbackEdgeViewBG];
    [self.view addSubview:_lineView];
    [self.view addSubview:_playbackLineView];
    [self.view addSubview:_topLeftRegion];
    [self.view addSubview:_topCenterRegion];
    [self.view addSubview:_topRightRegion];
    [self.view addSubview:_bottomLeftRegion];
    [self.view addSubview:_bottomCenterRegion];
    [self.view addSubview:_bottomRightRegion];
    
    [self.view addSubview:_artistTitle];
    [self.view addSubview:_songTitle];
    [self.view addSubview:_albumTitle];
    
    [self.view addSubview:_actionHUD];
    [self.view addSubview:_nightTimeFade];
    
    [self.view addSubview:_gpsDistanceRemaining];
    _gpsDistanceRemaining.frame=CGRectMake(10,0,100,30);
    _gpsDistanceRemaining.textColor = [UIColor blackColor];
    [_gpsDistanceRemaining setAlpha:0.5];

    _lineView.backgroundColor = [UIColor clearColor];
    _edgeViewBG.backgroundColor = [UIColor clearColor];
    _playbackLineView.backgroundColor = [UIColor clearColor];
    _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
    _nightTimeFade.backgroundColor = [UIColor clearColor];
    _actionHUD.backgroundColor = [UIColor clearColor];
    _actionHUD.textColor = [UIColor clearColor];
    _actionHUD.userInteractionEnabled=NO;
    _nightTimeFade.frame=CGRectMake(0, 0, 600, 600);
    _bgView.frame=CGRectMake(0, 0, 600, 600);

    
    UIPanGestureRecognizer *recognizer = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleSwipe:)];
    [self.view addGestureRecognizer:recognizer];
    
    NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
    MPMusicPlayerController *mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    
    [notificationCenter addObserver:self
                           selector:@selector(nowPlayingItemChanged:)
                               name:MPMusicPlayerControllerNowPlayingItemDidChangeNotification
                             object:mediaPlayer];
    
    [mediaPlayer beginGeneratingPlaybackNotifications];
    [[NSNotificationCenter defaultCenter]
     addObserver:self
     selector:@selector(deviceOrientationDidChangeNotification:)
     name:UIDeviceOrientationDidChangeNotification
     object:nil];
    
/*    //notifier for orientation changes
    [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
    [[NSNotificationCenter defaultCenter]
     addObserver:self selector:@selector(orientationChanged:)
     name:UIDeviceOrientationDidChangeNotification
     object:[UIDevice currentDevice]];
 */

    //init gps
    if ([[defaults objectForKey:@"GPSVolume"] isEqual:@"YES"]) {
        [self startGPSVolume];
    }
    
    self.bannerIsVisible = NO;
//    _speedTier = 0;
    
    if ([[defaults objectForKey:@"firstRun"] isEqual:@"QS"]) {
        [defaults setObject:@"done" forKey:@"firstRun"];
        [defaults synchronize];
        [self showInstructions];
    }
}

- (void) initAdBanner {
    if (!adBanner) {
        adBanner = [[ADBannerView alloc] initWithFrame:CGRectMake(0,self.view.bounds.size.height,self.view.bounds.size.width,[self getBannerHeight])];
        self.bannerIsVisible = NO;
        adBanner.delegate = self;
        [self.view addSubview:adBanner];
    }
}

-(void) startPlaybackWatcher {
    if ([self.scrubTimer isValid]) [self.scrubTimer invalidate];
    self.scrubTimer = [NSTimer scheduledTimerWithTimeInterval:   0.05f
                                                  target: self
                                                selector: @selector(updatePlaybackHUD)
                                                userInfo: nil
                                                 repeats: YES];
}


-(void) nowPlayingItemChanged:(NSNotification *)notification {
    MPMusicPlayerController *mediaPlayer = (MPMusicPlayerController *)notification.object;

    [self scrollingTimerKiller]; [self startMarqueeTimer];
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]!=NULL)
        [self setGlobalColors];
    MPMediaItem *song = [mediaPlayer nowPlayingItem];
    if (song) {
        NSString *title = [song valueForProperty:MPMediaItemPropertyTitle];
        NSString *album = [song valueForProperty:MPMediaItemPropertyAlbumTitle];
        NSString *artist = [song valueForProperty:MPMediaItemPropertyArtist];
        NSString *playCount = [song valueForProperty:MPMediaItemPropertyPlayCount];
/*
        NSURL* songURL = [song valueForProperty:MPMediaItemPropertyAssetURL];
        AVAsset* songAsset = [AVURLAsset URLAssetWithURL:songURL options:nil];
        NSString* lyrics = [songAsset lyrics];
 */
        
        NSLog(@"title: %@", title);
        NSLog(@"album: %@", album);
        NSLog(@"artist: %@", artist);
        NSLog(@"playCount: %@", playCount);
//        NSLog(@"lyrics: %@",lyrics);
    }
}


- (void) setThemeColors {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    
    NSString *currentTheme = [defaults objectForKey:@"currentTheme"];
    NSMutableDictionary *themedict = [gestureController themes];
    NSArray *themecolors = [themedict objectForKey:currentTheme];
    _themeBG = [themecolors objectAtIndex:0];
    _themeColorArtist = [themecolors objectAtIndex:1];
    _themeColorSong = [themecolors objectAtIndex:1];
    _themeColorAlbum = [themecolors objectAtIndex:1];
    [self invertColorsIfNecessary];
}

- (void) setGlobalColors {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    
    MPMediaItemArtwork *artwork = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtwork];
    if ((artwork != nil) & ([[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"])) {
        // so do a second check to see if it has at least 50x50 pixels
        if ([artwork imageWithSize:CGSizeMake(50,50)]) {
            _albumArt.image = [artwork imageWithSize:CGSizeMake(self.view.bounds.size.width,self.view.bounds.size.height)];

            if ([[defaults objectForKey:@"showMap"] isEqual:@"YES"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) {
                _albumArt.alpha = [[defaults objectForKey:@"AlbumArtFade"] floatValue];
            }
            else if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]) _albumArt.alpha = 1;
            else _albumArt.alpha = [[defaults objectForKey:@"AlbumArtFade"] floatValue];

            if ([[defaults objectForKey:@"AlbumArtScale"] isEqual:@"0"]) _albumArt.contentMode = UIViewContentModeScaleAspectFill;
            else if ([[defaults objectForKey:@"AlbumArtScale"] isEqual:@"1"]) _albumArt.contentMode = UIViewContentModeScaleAspectFit;

            
            if ([[defaults objectForKey:@"albumArtColors"] isEqual:@"YES"]) {
                LEColorPicker *colorPicker = [[LEColorPicker alloc] init];
                LEColorScheme *colorScheme = [colorPicker colorSchemeFromImage:[artwork imageWithSize:CGSizeMake(40,40)]];
                
                if (colorScheme!=nil) {
                    const CGFloat* components = CGColorGetComponents([colorScheme primaryTextColor].CGColor);
                    float pred=components[0];
                    float pgreen=components[1];
                    float pblue=components[2];
                
                    float sred,sgreen,sblue;
                // If it can't find a good secondary color, LEColorPicker returns UIColors which can't be averaged, so substitute RGB black or white; else grab the color for averaging
                    if ([[colorScheme secondaryTextColor] isEqual:[UIColor blackColor]]) {
                        sred=0; sgreen=0; sblue=0; }
                    else if ([[colorScheme secondaryTextColor] isEqual:[UIColor whiteColor]]) {
                        sred=1; sgreen=1; sblue=1; }
                    else {
                        components = CGColorGetComponents([colorScheme secondaryTextColor].CGColor);
                        sred=components[0];
                        sgreen=components[1];
                        sblue=components[2];
                    }
                    _themeBG = [colorScheme backgroundColor];
                    _themeColorArtist = [colorScheme primaryTextColor];
                    _themeColorSong = [UIColor colorWithRed: (sred+(pred*2))/3   green: (sgreen+(pgreen*2))/3   blue:(sblue+(pblue*2))/3   alpha:1];
                    _themeColorAlbum = [colorScheme secondaryTextColor];
                }
            } else [self setThemeColors];
        } else { _albumArt.alpha = 0.0f; [self setThemeColors]; }
        
    }  else { _albumArt.alpha = 0.0f; [self setThemeColors]; }
}

- (void) invertColorsIfNecessary {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSDate *currentTime = [NSDate date];
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"HH"];
    NSString *resultString = [dateFormatter stringFromDate: currentTime];
    
    UIColor *temp;
    float theHour = [resultString floatValue];
    float sundown = (int)[[defaults objectForKey:@"SunSetHour"] floatValue]; float sunup = (int)[[defaults objectForKey:@"SunRiseHour"] floatValue];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"] | ([[defaults objectForKey:@"InvertAtNight"] isEqual:@"YES"] & ((theHour>sundown) | (theHour < sunup)))) {
        temp = _themeBG;
        _themeBG = _themeColorSong;
        _themeColorArtist = temp;
        _themeColorSong = temp;
        _themeColorAlbum = temp;
    }
}

/*** HUD display setups ********************************************************************************************************************************************/

-(void) updatePlaybackHUD {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]!=NULL) {
        [self setGlobalColors];

        float red, green, blue, alpha;
        float red2, green2, blue2, alpha2;

        [_themeColorArtist getRed:&red green:&green blue:&blue alpha:&alpha];
        [_themeBG getRed:&red2 green:&green2 blue:&blue2 alpha:&alpha2];

        long totalPlaybackTime = [[[mediaPlayer nowPlayingItem] valueForProperty: @"playbackDuration"] longValue];

        float playbackPosition=(self.view.bounds.size.width*([mediaPlayer currentPlaybackTime]/totalPlaybackTime));
        int scrubTop = 0;
        int scrubLeft = 0;
        if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]) {
            if (UIInterfaceOrientationIsLandscape(_activeOrientation)) {
                if ([[defaults objectForKey:@"showMap"] isEqual:@"YES"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) scrubLeft = (self.view.bounds.size.width/2);
                else if ([[defaults objectForKey:@"AlbumArtScale"] isEqual:@"0"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) scrubLeft = (self.view.bounds.size.width/2)+36;
                else scrubLeft = (self.view.bounds.size.width/2);
                playbackPosition = ((self.view.bounds.size.width/2)-36)*([mediaPlayer currentPlaybackTime]/totalPlaybackTime) + scrubLeft;
            } else {
                if ([[defaults objectForKey:@"showMap"] isEqual:@"YES"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) scrubTop = (self.view.bounds.size.height/2);
                    else scrubTop = (self.view.bounds.size.height/2)+36;
            }
        }

        _playbackLineView.backgroundColor = [UIColor clearColor];
        _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
        
        long height;
        height = self.view.bounds.size.height-[self getBannerHeight];
        
        if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"0"]) {
            _playbackLineView.frame=CGRectMake(playbackPosition, scrubTop,  self.view.bounds.size.width, self.view.bounds.size.height);
            _playbackLineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.35f];
            _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
        } else if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"1"]) {
            _playbackLineView.frame = CGRectMake(playbackPosition, scrubTop, 15, self.view.bounds.size.height);
            _playbackLineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.35f];
            _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
        } else if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"2"]) {
            _playbackLineView.frame = CGRectMake(playbackPosition, height-15, 15, 80);
            _playbackLineView.backgroundColor = [UIColor colorWithRed:red2 green:green2 blue:blue2 alpha:1.f];
            _playbackEdgeViewBG.frame = CGRectMake(scrubLeft, height-15, self.view.bounds.size.width, 80);
            _playbackEdgeViewBG.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.5f];
        }
    }
    
    // since this runs 5 times a second, update volume per GPS here
    mediaPlayer.volume=_volumeTarget;
    [self setupHUD];
    [self setupLabels];
}

- (void)setupLabels {
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
//    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    [self setupFramesAndMargins];
    
    // dim display if it's night and dim-at-night is on
    NSDate *currentTime = [NSDate date];
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"HH"];
    NSString *resultString = [dateFormatter stringFromDate: currentTime];
    float theHour = [resultString floatValue];
    [dateFormatter setDateFormat:@"mm"];
    resultString = [dateFormatter stringFromDate: currentTime];
    float theMinute = [resultString floatValue];
    float sundown = (int)[[defaults objectForKey:@"SunSetHour"] floatValue]; float sunup = (int)[[defaults objectForKey:@"SunRiseHour"] floatValue];
    if (((theHour>=sundown) | (theHour < sunup)) & ([[defaults objectForKey:@"DimAtNight" ] isEqual:@"YES"])) {
        // fade towards half-dark during the hour after sundown
        if (theHour==sundown) {
            _nightTimeFade.backgroundColor = [[UIColor blackColor] colorWithAlphaComponent:0+((theMinute/60)/2)];
        } // or fade in before sunup
        else if (theHour==sunup-1) {
            _nightTimeFade.backgroundColor = [[UIColor blackColor] colorWithAlphaComponent:0.5-((theMinute/60)/2)];
        } // if the middle of the night, go half dark
        else _nightTimeFade.backgroundColor = [[UIColor blackColor] colorWithAlphaComponent:0.5f];
    } else _nightTimeFade.backgroundColor = [UIColor clearColor];
    
    //setup colors and alignment and font sizing
    switch ((int)[[defaults objectForKey:@"artistAlignment"] floatValue]) {
        case 0: _artistTitle.textAlignment = NSTextAlignmentLeft; break;
        case 1: _artistTitle.textAlignment = NSTextAlignmentCenter;  break;
        case 2: _artistTitle.textAlignment = NSTextAlignmentRight;  break;
    }
    switch ((int)[[defaults objectForKey:@"songAlignment"] floatValue]) {
        case 0: _songTitle.textAlignment = NSTextAlignmentLeft; break;
        case 1: _songTitle.textAlignment = NSTextAlignmentCenter;  break;
        case 2: _songTitle.textAlignment = NSTextAlignmentRight;  break;
    }
    switch ((int)[[defaults objectForKey:@"albumAlignment"] floatValue]) {
        case 0: _albumTitle.textAlignment = NSTextAlignmentLeft; break;
        case 1: _albumTitle.textAlignment = NSTextAlignmentCenter;  break;
        case 2: _albumTitle.textAlignment = NSTextAlignmentRight;  break;
    }
    int artistFontSize = (int)[[defaults objectForKey:@"artistFontSize"] floatValue];
    int songFontSize = (int)[[defaults objectForKey:@"songFontSize"] floatValue];
    int albumFontSize = (int)[[defaults objectForKey:@"albumFontSize"] floatValue];

    // halve text height in portrait if set to do so
    if (((self.view.bounds.size.height==480) | (self.view.bounds.size.height==568)) & [[defaults objectForKey:@"titleShrinkInPortrait"] isEqual:@"YES"]) {
        artistFontSize = (int)artistFontSize/2;
        if (artistFontSize<(int)[[defaults objectForKey:@"minimumFontSize"] floatValue]) artistFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
        songFontSize = (int)songFontSize/2;
        if (songFontSize<(int)[[defaults objectForKey:@"minimumFontSize"] floatValue]) songFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
        albumFontSize = (int)albumFontSize/2;
        if (albumFontSize<(int)[[defaults objectForKey:@"minimumFontSize"] floatValue]) albumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
    }

    
    //actually do the drawing
    _bgView.backgroundColor = _themeBG;
    NSString *artistString = [NSString stringWithFormat:@""];
    NSString *songString = [NSString stringWithFormat:@""];
    NSString *albumString = [NSString stringWithFormat:@""];
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) { //output "nothing playing screen" if nothing
        artistString = @"No music playing.";
        songString = @"Tap for default playlist.";
        albumString = @"Long hold for menu.";
    } else {
        artistString   = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtist];
        songString   = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle];
        albumString    = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyAlbumTitle];
    }

    _artistTitle.numberOfLines = 1;
    _artistTitle.text   = artistString;
    _artistTitle.font   = [UIFont systemFontOfSize:artistFontSize];
    _artistTitle.textColor = _themeColorArtist;
    [_artistTitle setAlpha:0.8f];
    _artistTitle.minimumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
        
        
//        int songOffset = _songTitle.frame.origin.y;
    int songOffset = topMargin+(((self.view.bounds.size.height-(topMargin+bottomMargin))/2)-(_songTitle.frame.size.height/2));
//        if (self.bannerIsVisible) songOffset=(self.view.bounds.size.height/2)-(_songTitle.frame.size.height/2);//-([self getBannerHeight]/2);
        // do not replace song title label if the scrolling marquee is handling that right now
    if (_timersRunning==0) {
        _songTitle.frame=CGRectMake(leftMargin-_marqueePosition, songOffset-([self getBannerHeight]/2), self.view.bounds.size.width-(leftMargin+rightMargin), (int)[[defaults objectForKey:@"songFontSize"] floatValue]+15);
        if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]) _songTitle.numberOfLines = 2; else _songTitle.numberOfLines = 1;
        _songTitle.text   = songString;
        _songTitle.font   = [UIFont systemFontOfSize:songFontSize];
        _songTitle.textColor = _themeColorSong;
        _songTitle.minimumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
    }
        
    int albumOffset = self.view.bounds.size.height-bottomMargin-_albumTitle.frame.size.height; //_albumTitle.frame.origin.y;
    if (self.bannerIsVisible) albumOffset=(self.view.bounds.size.height-bottomMargin-_albumTitle.frame.size.height)-[self getBannerHeight]; //20 is bottom margin
    if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"2"]) albumOffset = albumOffset-15;
    _albumTitle.frame=CGRectMake(leftMargin,albumOffset,self.view.bounds.size.width-(leftMargin+rightMargin),(int)[[defaults objectForKey:@"albumFontSize"] floatValue]+15);
    _albumTitle.numberOfLines = 1;
    _albumTitle.font    = [UIFont systemFontOfSize:albumFontSize];
    _albumTitle.text    = albumString;
    _albumTitle.textColor = _themeColorAlbum;
    [_albumTitle setAlpha:0.8f];
    _albumTitle.minimumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
//        if (self.bannerIsVisible) _albumTitle.frame = CGRectOffset(_albumTitle.frame, 0, -[self getBannerHeight]);

    if ([[defaults objectForKey:@"titleShrinkLong"] isEqual:@"YES"]) [self drawFittedText];
}

- (void) setupDefaultFrames {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    _artistTitle.frame = CGRectMake(leftMargin,topMargin,self.view.bounds.size.width-(leftMargin+rightMargin),(int)[[defaults objectForKey:@"artistFontSize"] floatValue]+15);
    
    artistPosition = topMargin;
    songPosition = 0;
    albumPosition = 0;
    
    _albumArt.frame = self.view.bounds;
    _map.frame = self.view.bounds;
}

- (void) setupFramesAndMargins {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    
    leftMargin = 20;
    rightMargin = 20;
    topMargin = 20;
    bottomMargin = 20;
    
    
    if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"0"]) { // overlay set up is simple
        [self setupDefaultFrames];
    }
    else { // side by side
        // if artwork on and no artwork, revert to full width
        MPMediaItemArtwork *artwork = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtwork];
        if (![artwork imageWithSize:CGSizeMake(50,50)]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]&[[defaults objectForKey:@"showMap"] isEqual:@"NO"]) [self setupDefaultFrames];
        else if (UIInterfaceOrientationIsLandscape(_activeOrientation)) { // side by side landscape
            leftMargin = (self.view.bounds.size.width/2)+20;
            topMargin = 20;
            if ([[defaults objectForKey:@"AlbumArtScale"] isEqual:@"0"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) leftMargin=leftMargin+40;  //if art scale is "fill" it is wider than "fit" or map view
            else if ([[defaults objectForKey:@"AlbumArtScale"] isEqual:@"1"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) leftMargin=leftMargin+20;  //if art scale is "fill" it is wider than "fit" or map view
            
            artistPosition = topMargin;
            
            _albumArt.frame = CGRectMake(18,0, self.view.bounds.size.width/2,self.view.bounds.size.height);
            _map.frame = CGRectMake(0,0, self.view.bounds.size.width/2,self.view.bounds.size.height);
            if ([[defaults objectForKey:@"showMap"] isEqual:@"YES"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) {
                _albumArt.frame = CGRectMake(self.view.bounds.size.width/2,0, self.view.bounds.size.width/2,self.view.bounds.size.height);
                leftMargin = (self.view.bounds.size.width/2)+20;
            }
        } else { // side by side portrait
            // the 'full' size produces an odd frame. compensate.
            if ([[defaults objectForKey:@"AlbumArtScale"] isEqual:@"0"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) {
                topMargin = (self.view.bounds.size.height/2)+58;
                _albumArt.frame = CGRectMake(0,18, self.view.bounds.size.width,self.view.bounds.size.height/2);
            }
            else {
                topMargin = (self.view.bounds.size.height/2)+20;
                _albumArt.frame = CGRectMake(0,0, self.view.bounds.size.width,self.view.bounds.size.height/2);
            }
            _map.frame = CGRectMake(0,0, self.view.bounds.size.width,self.view.bounds.size.height/2);
            if ([[defaults objectForKey:@"showMap"] isEqual:@"YES"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) {
                _albumArt.frame = CGRectMake(0,self.view.bounds.size.height/2, self.view.bounds.size.width,self.view.bounds.size.height/2);
                topMargin = (self.view.bounds.size.height/2)+20;
                leftMargin = 20;
            }
        }
    }
    // adjust for edge HUD layouts
    if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"2"]) bottomMargin=bottomMargin+15;
    if ([[defaults objectForKey:@"HUDType"] isEqual:@"3"]) leftMargin=leftMargin+15;
    
    [self drawCornerRegions];
    artistPosition = topMargin;
    if (topButtons) {
        artistPosition=artistPosition+20;
    }
    _artistTitle.frame = CGRectMake(leftMargin,artistPosition,self.view.bounds.size.width-(leftMargin+rightMargin),(int)[[defaults objectForKey:@"artistFontSize"] floatValue]+15);
}

- (void) drawCornerRegions {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    topButtons = NO;
    if (![[defaults objectForKey:@"TopLeft"] isEqual:@"Unassigned"]) {
        _topLeftRegion.text = [self actionSymbol:[defaults objectForKey:@"TopLeft"] ];
        if ([_topLeftRegion.text sizeWithFont:[UIFont systemFontOfSize:30]].width>(self.view.bounds.size.width-(leftMargin+rightMargin))/3) _topLeftRegion.font = [UIFont systemFontOfSize:18]; else _topLeftRegion.font = [UIFont systemFontOfSize:30];
        _topLeftRegion.frame = CGRectMake(leftMargin,topMargin-20,(self.view.bounds.size.width-(leftMargin+rightMargin))/3,50);
        _topLeftRegion.textColor = _themeColorSong;
        _topLeftRegion.numberOfLines=0;
        _topLeftRegion.lineBreakMode=NSLineBreakByTruncatingTail;
        [_topLeftRegion setAlpha:0.65f];
        topButtons = YES;
    } else _topLeftRegion.text = @"";
    if (![[defaults objectForKey:@"TopCenter"] isEqual:@"Unassigned"]) {
        _topCenterRegion.text = [self actionSymbol:[defaults objectForKey:@"TopCenter"] ];
        if ([_topCenterRegion.text sizeWithFont:[UIFont systemFontOfSize:30]].width>(self.view.bounds.size.width-(leftMargin+rightMargin))/3) _topCenterRegion.font = [UIFont systemFontOfSize:18]; else _topCenterRegion.font = [UIFont systemFontOfSize:30];
        _topCenterRegion.frame = CGRectMake(leftMargin+(self.view.bounds.size.width-(leftMargin+rightMargin))/3,topMargin-20,(self.view.bounds.size.width-(leftMargin+rightMargin))/3,50);
        _topCenterRegion.textColor = _themeColorSong;
        _topCenterRegion.numberOfLines=0;
        _topCenterRegion.lineBreakMode=NSLineBreakByTruncatingTail;
        _topCenterRegion.textAlignment = NSTextAlignmentCenter;
        [_topCenterRegion setAlpha:0.65f];
        topButtons = YES;
    } else _topCenterRegion.text = @"";
    if (![[defaults objectForKey:@"TopRight"] isEqual:@"Unassigned"]) {
        _topRightRegion.text = [self actionSymbol:[defaults objectForKey:@"TopRight"] ];
        if ([_topRightRegion.text sizeWithFont:[UIFont systemFontOfSize:30]].width>(self.view.bounds.size.width-(leftMargin+rightMargin))/3) _topRightRegion.font = [UIFont systemFontOfSize:18]; else _topRightRegion.font = [UIFont systemFontOfSize:30];
        _topRightRegion.frame = CGRectMake(self.view.bounds.size.width-((self.view.bounds.size.width-(leftMargin+rightMargin))/3+rightMargin),topMargin-20,(self.view.bounds.size.width-(leftMargin+rightMargin))/3,50);
        _topRightRegion.textColor = _themeColorSong;
        _topRightRegion.numberOfLines=0;
        _topRightRegion.lineBreakMode=NSLineBreakByTruncatingTail;
        _topRightRegion.textAlignment = NSTextAlignmentRight;
        [_topRightRegion setAlpha:0.65f];
        topButtons = YES;
    } else _topRightRegion.text = @"";

    // bottom row
    int playbackBarMargin = 50;
    if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"2"]) playbackBarMargin = 65;
    if (![[defaults objectForKey:@"BottomLeft"] isEqual:@"Unassigned"]) {
        _bottomLeftRegion.text = [self actionSymbol:[defaults objectForKey:@"BottomLeft"] ];
        if ([_bottomLeftRegion.text sizeWithFont:[UIFont systemFontOfSize:30]].width>(self.view.bounds.size.width-(leftMargin+rightMargin))/3) _bottomLeftRegion.font = [UIFont systemFontOfSize:18]; else _bottomLeftRegion.font = [UIFont systemFontOfSize:30];
        _bottomLeftRegion.frame = CGRectMake(leftMargin,self.view.bounds.size.height-playbackBarMargin-[self getBannerHeight],(self.view.bounds.size.width-(leftMargin+rightMargin))/3,50);
        _bottomLeftRegion.textColor = _themeColorSong;
        _bottomLeftRegion.numberOfLines=0;
        _bottomLeftRegion.lineBreakMode=NSLineBreakByTruncatingTail;
        [_bottomLeftRegion setAlpha:0.65f];
        bottomMargin = 40;
    } else _bottomLeftRegion.text = @"";
    if (![[defaults objectForKey:@"BottomCenter"] isEqual:@"Unassigned"]) {
        _bottomCenterRegion.text = [self actionSymbol:[defaults objectForKey:@"BottomCenter"] ];
//        if ((UIInterfaceOrientationIsLandscape(_activeOrientation))) _bottomCenterRegion.font = [UIFont systemFontOfSize:30]; else _bottomCenterRegion.font = [UIFont systemFontOfSize:18];
        if ([_bottomCenterRegion.text sizeWithFont:[UIFont systemFontOfSize:30]].width>(self.view.bounds.size.width-(leftMargin+rightMargin))/3) _bottomCenterRegion.font = [UIFont systemFontOfSize:18]; else _bottomCenterRegion.font = [UIFont systemFontOfSize:30];
        
        _bottomCenterRegion.frame = CGRectMake(leftMargin+(self.view.bounds.size.width-(leftMargin+rightMargin))/3,self.view.bounds.size.height-playbackBarMargin-[self getBannerHeight],(self.view.bounds.size.width-(leftMargin+rightMargin))/3,50);
        _bottomCenterRegion.textColor = _themeColorSong;
        _bottomCenterRegion.numberOfLines=0;
        _bottomCenterRegion.lineBreakMode=NSLineBreakByTruncatingTail;
        _bottomCenterRegion.textAlignment = NSTextAlignmentCenter;
        [_bottomCenterRegion setAlpha:0.65f];
        bottomMargin = 40;
    } else _bottomCenterRegion.text = @"";
    if (![[defaults objectForKey:@"BottomRight"] isEqual:@"Unassigned"]) {
        _bottomRightRegion.text = [self actionSymbol:[defaults objectForKey:@"BottomRight"] ];
        if ([_bottomRightRegion.text sizeWithFont:[UIFont systemFontOfSize:30]].width>(self.view.bounds.size.width-(leftMargin+rightMargin))/3) _bottomRightRegion.font = [UIFont systemFontOfSize:18]; else _bottomRightRegion.font = [UIFont systemFontOfSize:30];
        _bottomRightRegion.frame = CGRectMake(self.view.bounds.size.width-((self.view.bounds.size.width-(leftMargin+rightMargin))/3+rightMargin),self.view.bounds.size.height-playbackBarMargin-[self getBannerHeight],(self.view.bounds.size.width-(leftMargin+rightMargin))/3,50);
        _bottomRightRegion.textColor = _themeColorSong;
        _bottomRightRegion.numberOfLines=0;
        _bottomRightRegion.lineBreakMode=NSLineBreakByTruncatingTail;
        _bottomRightRegion.textAlignment = NSTextAlignmentRight;
        [_bottomRightRegion setAlpha:0.65f];
        bottomMargin = 40;
    } else _bottomRightRegion.text = @"";
}

-(NSString*)actionSymbol:(NSString*)action {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    if ([action isEqual:@"Rewind"]) return @"\u2190\u20DD";
    else if ([action isEqual:@"FastForward"]) return @"\u2192\u20DD";
    else if ([action isEqual:@"Play"]) return @"\u25b8\u20DD";
    else if ([action isEqual:@"Pause"]) return @"\u05f2\u20DD";
    else if ([action isEqual:@"PlayPause"]&(mediaPlayer.playbackState==MPMusicPlaybackStatePlaying)) return @"\u05f2\u20dd"; // @"\u220e\u220e";
    else if ([action isEqual:@"PlayPause"]&(mediaPlayer.playbackState!=MPMusicPlaybackStatePlaying)) return @"\u25b8\u20DD";
    else if ([action isEqual:@"VolumeUp"]) return @"\u2191\u20DD";
    else if ([action isEqual:@"VolumeDown"]) return @"\u2193\u20DD";
    else if ([action isEqual:@"Next"]) return @"\u21c9\u20DD";
    else if ([action isEqual:@"Previous"]|[action isEqual:@"RestartPrevious"]) return @"\u21c7\u20DD";
    else if ([action isEqual:@"Menu"]) return @"\u2699";
    else if ([action isEqual:@"SongPicker"]) return @"\u23cf\u20DD";
    else if ([action isEqual:@"ToggleShuffle"]&[[defaults objectForKey:@"shuffle"] isEqual:@"YES"]) return @"\u21dd\u20DD";
    else if ([action isEqual:@"ToggleShuffle"]&[[defaults objectForKey:@"shuffle"] isEqual:@"NO"]) return @"\u2799\u20DD";
    else if ([action isEqual:@"ToggleRepeat"]&[[defaults objectForKey:@"repeat"] isEqual:@"YES"]) return @"\u221e\u20DD";
    else if ([action isEqual:@"ToggleRepeat"]&[[defaults objectForKey:@"repeat"] isEqual:@"NO"]) return @"\u223e\u20DD";
    else if ([action isEqual:@"StartDefaultPlaylist"]) return [NSString stringWithFormat:@"Play %@",[defaults objectForKey:@"playlist"] ];
    else if ([action isEqual:@"PlayCurrentAlbum"]) {
        if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]!=NULL)
        return [NSString stringWithFormat:@"Play %@",[mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyAlbumTitle] ];
        else return @"Unknown Album";
    }
    else if ([action isEqual:@"PlayCurrentArtist"]) {
        if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]!=NULL)
        return [NSString stringWithFormat:@"Play %@",[mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtist] ];
        else return @"Unkown Artist";
    }
    else if ([action isEqual:@"IncreaseRating"]|[action isEqual:@"DecreaseRating"]) { MPMediaItem *song = [mediaPlayer nowPlayingItem]; int rating = (int)[[song valueForKey:@"rating"] floatValue]; return [self ratingStars:rating]; }
    else if ([action isEqual:@"ShowQuickStart"]) return @"?\u20DD";
    else if ([action isEqual:@"NavigateHome"]) return @"\u2302\u20DD";
    else if ([action isEqual:@"NavigateToWork"]) return @"⚒\u20DD";
    else if ([action isEqual:@"NavigateToContact"]) return @"\u21ac\u20DD";
    return action;
}

-(NSString*)ratingStars:(int)rating {
    NSString *starString = @"";
    for (int i = 0; i<rating; i++) { starString=[starString stringByAppendingString:@"\u2605"];}
    return starString;
}
                                                                                                                        
-(void)drawFittedText {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    int minFontSize = (int)[[defaults objectForKey:@"minimumFontSize"] floatValue];

    int songFontSize = (int)[[defaults objectForKey:@"artistFontSize"] floatValue];
    long textHeight = [_artistTitle.text sizeWithFont:[_artistTitle font]].height;
	while( textHeight > minFontSize )
	{
        if ([_artistTitle.text sizeWithFont:[_artistTitle font]].width<self.view.bounds.size.width-(leftMargin+rightMargin)) break;
		[_artistTitle setFont:[[_artistTitle font] fontWithSize:--songFontSize]];
		textHeight = [_artistTitle.text sizeWithFont:[_artistTitle font]].height;
	}
 
    songFontSize = (int)[[defaults objectForKey:@"songFontSize"] floatValue];
    textHeight = [_songTitle.text sizeWithFont:[_songTitle font]].height;
	while( textHeight > minFontSize )
	{
        if ([_songTitle.text sizeWithFont:[_songTitle font]].width<self.view.bounds.size.width-(leftMargin+rightMargin)) break;
		[_songTitle setFont:[[_songTitle font] fontWithSize:--songFontSize]];
		textHeight = [_songTitle.text sizeWithFont:[_songTitle font]].height;
	}
    
    songFontSize = (int)[[defaults objectForKey:@"albumFontSize"] floatValue];
    textHeight = [_albumTitle.text sizeWithFont:[_albumTitle font]].height;
	while( textHeight > minFontSize )
	{
        if ([_albumTitle.text sizeWithFont:[_albumTitle font]].width<self.view.bounds.size.width-(leftMargin+rightMargin)) break;
		[_albumTitle setFont:[[_albumTitle font] fontWithSize:--songFontSize]];
		textHeight = [_albumTitle.text sizeWithFont:[_albumTitle font]].height;
	}
}


-(void) fadeActionHUDloop {
//    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    _actionHUD.backgroundColor = [[UIColor blackColor] colorWithAlphaComponent:_fadeActionHUDAlpha];
    _actionHUD.textColor = [[UIColor whiteColor] colorWithAlphaComponent:_fadeActionHUDAlpha];
    _fadeActionHUDAlpha = _fadeActionHUDAlpha-0.05f;
    if (_fadeActionHUDAlpha <= 0 ) {
        [self fadeActionHUDTimerKiller];
        _actionHUD.backgroundColor = [UIColor clearColor];
        _actionHUD.textColor = [UIColor clearColor];

    }
    //    NSLog(@"fading alpha is %f",_fadeHUDalpha);
    
}

-(void) fadeHUDloop {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    const CGFloat* components = CGColorGetComponents(_lineView.backgroundColor.CGColor);
    NSLog(@"Red: %f", components[0]);
    NSLog(@"Green: %f", components[1]);
    NSLog(@"Blue: %f", components[2]);
    NSLog(@"Alpha: %f", CGColorGetAlpha(_lineView.backgroundColor.CGColor));
    float red=components[0];
    float green=components[1];
    float blue=components[2];
    components = CGColorGetComponents(_edgeViewBG.backgroundColor.CGColor);
    float edgered=components[0];
    float edgegreen=components[1];
    float edgeblue=components[2];

    if ([[defaults objectForKey:@"HUDType"] isEqual:@"3"]) _edgeViewBG.backgroundColor = [UIColor colorWithRed:edgered green:edgegreen blue:edgeblue alpha:_fadeHUDalpha];
    else _lineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:_fadeHUDalpha];
    _fadeHUDalpha = _fadeHUDalpha-0.05f;
    if (_fadeHUDalpha <= 0 ) {
        [self fadeHUDTimerKiller];
        _lineView.backgroundColor = [UIColor clearColor];
        _edgeViewBG.backgroundColor = [UIColor clearColor];
    }
//    NSLog(@"fading alpha is %f",_fadeHUDalpha);
    
}

-(void) fadeHUD {
    self.fadeHUDTimer = [NSTimer scheduledTimerWithTimeInterval: 0.05f
                                                         target: self
                                                       selector: @selector(fadeHUDloop)
                                                       userInfo: nil
                                                        repeats: YES];
}

-(void) fadeActionHUD {
    self.actionHUDFadeTimer = [NSTimer scheduledTimerWithTimeInterval: 0.05f
                                                         target: self
                                                       selector: @selector(fadeActionHUDloop)
                                                       userInfo: nil
                                                        repeats: YES];
}

-(void)fadeActionHUDTimerKiller {
    if ( [[self actionHUDFadeTimer] isValid]){
        [[self actionHUDFadeTimer] invalidate];
    }
}

-(void)fadeHUDTimerKiller {
    if ( [[self fadeHUDTimer] isValid]){
        [[self fadeHUDTimer] invalidate];
    }
}

- (void) scrubTimerKiller {
     if ([self.scrubTimer isValid]) { [self.scrubTimer invalidate]; }
}

-(void) startActionHUDFadeTimer {
        [self fadeActionHUDTimerKiller];
    _fadeActionHUDAlpha = CGColorGetAlpha(_actionHUD.backgroundColor.CGColor);
        
        self.actionHUDFadeTimer = [NSTimer scheduledTimerWithTimeInterval: 0.75f
                                                             target: self
                                                           selector: @selector(fadeActionHUD)
                                                           userInfo: nil
                                                            repeats: NO];
}

-(void) startFadeHUDTimer {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    if ([[defaults objectForKey:@"VolumeAlwaysOn"] isEqual:@"NO"]) {
        [self fadeHUDTimerKiller];
        if ([[defaults objectForKey:@"HUDType"] isEqual:@"3"]) _fadeHUDalpha = CGColorGetAlpha(_edgeViewBG.backgroundColor.CGColor);
        else _fadeHUDalpha = CGColorGetAlpha(_lineView.backgroundColor.CGColor);

        self.fadeHUDTimer = [NSTimer scheduledTimerWithTimeInterval: 2.5f
                                                  target: self
                                                selector: @selector(fadeHUD)
                                                userInfo: nil
                                                 repeats: NO];
    }
}

-(void) drawActionHUD:(NSString*)action {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
//    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];

    if ([[defaults objectForKey:@"showActions"] isEqual:@"YES"]) {
        if (!([action isEqual:@"SongPicker"]|[action isEqual:@"Menu"]|[action isEqual:@"Unassigned"])) {
            _actionHUD.frame=CGRectMake((self.view.bounds.size.width/2)-80, (self.view.bounds.size.height/2)-80, 160, 160);
            _actionHUD.textAlignment=NSTextAlignmentCenter;
            _actionHUD.font = [UIFont systemFontOfSize:120];
            _actionHUD.textColor = [[UIColor whiteColor] colorWithAlphaComponent:0.4f];
            _actionHUD.backgroundColor = [[UIColor blackColor] colorWithAlphaComponent:0.4f];
            _actionHUD.layer.cornerRadius = 10;
            _actionHUD.lineBreakMode = NSLineBreakByClipping;
            _actionHUD.numberOfLines = 1;
            
            // a few pop-ups need special set-up, for font sizes for example, or adjustments to reflect the unset upcoming state (pop-up appears before action)
            if ([action isEqual:@"DecreaseRating"])  { _actionHUD.font=[UIFont systemFontOfSize:30]; MPMediaItem *song = [mediaPlayer nowPlayingItem]; int rating = (int)[[song valueForKey:@"rating"] floatValue]; if (rating==0) rating=1;  _actionHUD.text = [self ratingStars:rating-1];  }
            else if ([action isEqual:@"IncreaseRating"]) { _actionHUD.font=[UIFont systemFontOfSize:30]; MPMediaItem *song = [mediaPlayer nowPlayingItem]; int rating = (int)[[song valueForKey:@"rating"] floatValue]; if (rating==5) rating=4; _actionHUD.text = [self ratingStars:rating+1]; }
            else if ([action isEqual:@"ToggleShuffle"]) { if ([[defaults objectForKey:@"shuffle"] isEqual:@"YES"]) _actionHUD.text = @"\u2799"; else _actionHUD.text=@"\u21dd"; }
            else if ([action isEqual:@"ToggleRepeat"])  { if ([[defaults objectForKey:@"repeat"] isEqual:@"YES"]) _actionHUD.text = @"\u223e"; else _actionHUD.text=@"\u221e"; }
            else if ([action isEqual:@"PlayCurrentArtist"]) {
                _actionHUD.numberOfLines=0;
                if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]!=NULL)
                    _actionHUD.text = [NSString stringWithFormat:@"Playing\n%@",[mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtist]];
                else _actionHUD.text = [NSString stringWithFormat:@"Playing Unknown Artist"];
                _actionHUD.font=[UIFont systemFontOfSize:30]; }
            else if ([action isEqual:@"PlayCurrentAlbum"]) {
                _actionHUD.numberOfLines=0;
                if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]!=NULL)
                    _actionHUD.text = [NSString stringWithFormat:@"Playing\n%@",[mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyAlbumTitle] ];
                else _actionHUD.text = [NSString stringWithFormat:@"Playing Unknown Album"];
                _actionHUD.font=[UIFont systemFontOfSize:30];
                
            }
            else if ([action isEqual:@"StartDefaultPlaylist"]) { _actionHUD.numberOfLines=0; _actionHUD.text = [NSString stringWithFormat:@"Playing\n%@",[defaults objectForKey:@"playlist"]]; _actionHUD.font=[UIFont systemFontOfSize:30]; }
            else _actionHUD.text = [self actionSymbol:action];

            [self startActionHUDFadeTimer];
        }
    }
}

-(void) setupHUD {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
//    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];

    float red, green, blue, alpha;
    float red2, green2, blue2, alpha2;
    
    [_themeColorArtist getRed:&red green:&green blue:&blue alpha:&alpha];
    [_themeBG getRed:&red2 green:&green2 blue:&blue2 alpha:&alpha2];

    //adjust volume range for iAds
    long height;
    height = self.view.bounds.size.height-[self getBannerHeight];
    float volumeLevel=height-(height*_volumeBase);
    float targetVolumeLevel=height-(height*_volumeTarget);
    if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]&!UIInterfaceOrientationIsLandscape(_activeOrientation)) {
        height=(self.view.bounds.size.height/2)-[self getBannerHeight];
        volumeLevel=(height-(height*_volumeBase))+(self.view.bounds.size.height/2)+36;
        targetVolumeLevel=(height-(height*_volumeTarget))+(self.view.bounds.size.height/2)+35;
    }

    _lineView.backgroundColor = [UIColor clearColor];
    _edgeViewBG.backgroundColor = [UIColor clearColor];

    //setup for rectangle drawing display
    int leftSide = 0;
    int topSide = 0;
    
    if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]&UIInterfaceOrientationIsLandscape(_activeOrientation)) {
        // if fill scale, which is big, swell up 36px
        if ([[defaults objectForKey:@"showMap"] isEqual:@"YES"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) leftSide=self.view.bounds.size.width/2;
        else if ([[defaults objectForKey:@"AlbumArtScale"] isEqual:@"0"]&[[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"]) leftSide=self.view.bounds.size.width/2+36;
        else leftSide=self.view.bounds.size.width/2;
    }
    if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]&!UIInterfaceOrientationIsLandscape(_activeOrientation)) topSide=self.view.bounds.size.height/2+36;
    
    if ([[defaults objectForKey:@"HUDType"] isEqual:@"1"]) { //setup bar display
        _lineView.frame=CGRectMake(leftSide, volumeLevel, self.view.bounds.size.width, self.view.bounds.size.height);
        _lineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.10f];
        _edgeViewBG.frame = CGRectMake(leftSide, targetVolumeLevel, self.view.bounds.size.width, self.view.bounds.size.height);
        _edgeViewBG.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.25f];
    } else if ([[defaults objectForKey:@"HUDType"] isEqual:@"2"]) { // setup line display
        _lineView.frame = CGRectMake(leftSide, volumeLevel, self.view.bounds.size.width, 15);
        _lineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.10f];
        _edgeViewBG.frame = CGRectMake(leftSide, targetVolumeLevel, self.view.bounds.size.width, 15);
        _edgeViewBG.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.25f];
    } else if ([[defaults objectForKey:@"HUDType"] isEqual:@"3"]) { // setup edge display
        _lineView.frame = CGRectMake(self.view.bounds.size.width-15, volumeLevel, self.view.bounds.size.width, 15);
        _lineView.backgroundColor = [UIColor colorWithRed:red2 green:green2 blue:blue2 alpha:1.0f];
        _edgeViewBG.frame = CGRectMake(self.view.bounds.size.width-15, topSide, self.view.bounds.size.width, self.view.bounds.size.height);
        _edgeViewBG.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.5f];
    }
    [self startFadeHUDTimer];
}

-(void)setupSystemHUD {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ([[defaults objectForKey:@"HUDType"] isEqual:@"0"]) { // 0 is control index for System HUD, so turn it on by removing the volume view
        [_volume removeFromSuperview];
    } else { // or turn on volume view, and hide form sight
        _volume = [[MPVolumeView alloc] initWithFrame: CGRectMake(-100,-100,16,16)];
        _volume.showsRouteButton = NO;
        _volume.userInteractionEnabled = NO;
        [self.view addSubview:_volume];
    }

}


/*** Gesture Actions begin ************************************************************************************************************************/


- (void)handleSwipe:(UIPanGestureRecognizer *)gesture {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    CGPoint translation = [gesture translationInView:self.view];
    NSString *key,*keyContinuous;
    static int lastGestureFingers;

    _direction = directionNone;
    
// this useful debug info is spammy
//    NSLog(@"%lu touches",(unsigned long)gesture.numberOfTouches);
    if (gesture.state == UIGestureRecognizerStateBegan)
    {
        _direction = directionNone;
        lastGestureFingers = (unsigned long)gesture.numberOfTouches;
    }
// this is continuous gesture, bad for some actions, good for others.
//    else if (gesture.state == UIGestureRecognizerStateChanged)
// this is trigger-once actions
    else if (gesture.state == UIGestureRecognizerStateChanged && _direction == directionNone && (unsigned long)gesture.numberOfTouches==lastGestureFingers)
    {
        _direction = [self determineSwipeDirectiond:translation];
       switch (_direction) {
            case directionDown:
               key = [NSString stringWithFormat:@"%luSwipeDown",(unsigned long)gesture.numberOfTouches];
               keyContinuous = [NSString stringWithFormat:@"%luSwipeDownContinuous",(unsigned long)gesture.numberOfTouches];
               if ([[defaults objectForKey:keyContinuous] isEqual:@"YES"]) [self performPlayerAction:[defaults objectForKey:key]:key];
               else {
                   if (![[defaults objectForKey:@"doNotRepeat"] isEqual:key]) {
                       [self performPlayerAction:[defaults objectForKey:key]:key];
                       [defaults setObject:key forKey:@"doNotRepeat"];
                       self.DNRTimer = [NSTimer scheduledTimerWithTimeInterval: 0.5f
                                                                     target: self
                                                                   selector: @selector(resetDoNotRepeat)
                                                                   userInfo: nil
                                                                    repeats: NO];
                   }
               }
//               NSLog(@"Swipe gesture %@ is %@ continuous",key,[defaults objectForKey:keyContinuous]);
                break;
                
            case directionUp:
               key = [NSString stringWithFormat:@"%luSwipeUp",(unsigned long)gesture.numberOfTouches];
               keyContinuous = [NSString stringWithFormat:@"%luSwipeUpContinuous",(unsigned long)gesture.numberOfTouches];
               if ([[defaults objectForKey:keyContinuous] isEqual:@"YES"]) [self performPlayerAction:[defaults objectForKey:key]:key];
               else {
                   if (![[defaults objectForKey:@"doNotRepeat"] isEqual:key]) {
                       [self performPlayerAction:[defaults objectForKey:key]:key];
                       [defaults setObject:key forKey:@"doNotRepeat"];
                       self.DNRTimer = [NSTimer scheduledTimerWithTimeInterval: 0.5f
                                                                     target: self
                                                                   selector: @selector(resetDoNotRepeat)
                                                                   userInfo: nil
                                                                    repeats: NO];
                   }
               }
//               NSLog(@"Swipe gesture %@ is %@ continuous",key,[defaults objectForKey:keyContinuous]);
                break;
                
            case directionRight:
               key = [NSString stringWithFormat:@"%luSwipeRight",(unsigned long)gesture.numberOfTouches];
               keyContinuous = [NSString stringWithFormat:@"%luSwipeRightContinuous",(unsigned long)gesture.numberOfTouches];
               if (([[defaults objectForKey:keyContinuous] isEqual:@"YES"]) & ([[defaults objectForKey:@"doNotRepeat"] isEqual:@"narf!"])) [self performPlayerAction:[defaults objectForKey:key]:key];
               else {
                   if (![[defaults objectForKey:@"doNotRepeat"] isEqual:key]) {
                       [self performPlayerAction:[defaults objectForKey:key]:key];
                       [defaults setObject:key forKey:@"doNotRepeat"];
                       self.DNRTimer = [NSTimer scheduledTimerWithTimeInterval: 0.5f
                                                                     target: self
                                                                   selector: @selector(resetDoNotRepeat)
                                                                   userInfo: nil
                                                                    repeats: NO];
                   }
               }
                break;
                
            case directionLeft:
               key = [NSString stringWithFormat:@"%luSwipeLeft",(unsigned long)gesture.numberOfTouches];
               keyContinuous = [NSString stringWithFormat:@"%luSwipeLeftContinuous",(unsigned long)gesture.numberOfTouches];
               if (([[defaults objectForKey:keyContinuous] isEqual:@"YES"]) & ([[defaults objectForKey:@"doNotRepeat"] isEqual:@"narf!"])) [self performPlayerAction:[defaults objectForKey:key]:key];
               else {
                   if (![[defaults objectForKey:@"doNotRepeat"] isEqual:key]) {
                       [self performPlayerAction:[defaults objectForKey:key]:key];
                       [defaults setObject:key forKey:@"doNotRepeat"];
                       self.DNRTimer = [NSTimer scheduledTimerWithTimeInterval: 0.5f
                                                                     target: self
                                                                   selector: @selector(resetDoNotRepeat)
                                                                   userInfo: nil
                                                                    repeats: NO];
                   }
               }
                break;
                
            default:
                break;
       }
      //  _direction=directionNone;
        
    }
    else if (gesture.state == UIGestureRecognizerStateEnded) {
        _fingers=0;
        NSLog(@"Stop. Mediaplayer state is %ld",(long)[mediaPlayer playbackState]);
        //if ([mediaPlayer playbackState] == MPMusicPlaybackStateSeekingForward | [mediaPlayer playbackState]==MPMusicPlaybackStateSeekingBackward) [mediaPlayer endSeeking];
    }
    [defaults synchronize];
}


- (void) resetDoNotRepeat {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"narf!" forKey:@"doNotRepeat"];
    [defaults synchronize];
}

- (swipeDirections)determineSwipeDirectiond:(CGPoint)translation
{
  //  if (_direction != directionNone)
    //    return _direction;
    
    // determine if horizontal swipe only if you meet some minimum velocity
    
    if (fabs(translation.x) > gestureMinimumTranslation)
    {
        BOOL gestureHorizontal = NO;
        
        if (translation.y == 0.0)
            gestureHorizontal = YES;
        else
            gestureHorizontal = (fabs(translation.x / translation.y) > 5.0);
        
        if (gestureHorizontal)
        {
            if (translation.x > 0.0)
                return directionRight;
            else
                return directionLeft;
        }
    }
    // determine if vertical swipe only if you meet some minimum velocity
    
    else if (fabs(translation.y) > gestureMinimumTranslation)
    {
        BOOL gestureVertical = NO;
        
        if (translation.x == 0.0)
            gestureVertical = YES;
        else
            gestureVertical = (fabs(translation.y / translation.x) > 5.0);
        
        if (gestureVertical)
        {
            if (translation.y > 0.0)
                return directionDown;
            else
                return directionUp;
        }
    }
    
    return _direction;
}

- (IBAction)twoFingerTap:(id)sender {
/*    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"21Tap"]:@"21Tap"];
 */
}

- (IBAction)threeFingerTap:(id)sender {
/*    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"31Tap"]:@"31Tap"];
 */
}

- (IBAction)longPressDetected:(UIGestureRecognizer *)sender {
    if (sender.state == UIGestureRecognizerStateBegan) {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        [self performPlayerAction:[defaults objectForKey:@"1LongPress"]:@"1LongPress"];
    } // else, UIGestureRecognizerState[Changed / Ended]
}

- (IBAction)longPress2Detected:(UIGestureRecognizer *)sender {
    if (sender.state == UIGestureRecognizerStateBegan) {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        [self performPlayerAction:[defaults objectForKey:@"2LongPress"]:@"2LongPress"];
    } // else, UIGestureRecognizerState[Changed / Ended]7
}

- (IBAction)longPress3Detected:(UIGestureRecognizer *)sender {
    if (sender.state == UIGestureRecognizerStateBegan) {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        [self performPlayerAction:[defaults objectForKey:@"3LongPress"]:@"3LongPress"];
    } // else, UIGestureRecognizerState[Changed / Ended]
}


/*** tappytime ***/
-(void)oneFingerSingleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"11Tap"]:@"11Tap"];
}
-(void)oneFingerDoubleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"12Tap"]:@"12Tap"];
}
-(void)oneFingerTripleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"13Tap"]:@"13Tap"];
}
-(void)oneFingerQuadrupleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"14Tap"]:@"14Tap"];
}

-(void)twoFingerSingleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"21Tap"]:@"21Tap"];
}
-(void)twoFingerDoubleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"22Tap"]:@"22Tap"];
}
-(void)twoFingerTripleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"23Tap"]:@"23Tap"];
}
-(void)twoFingerQuadrupleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"24Tap"]:@"24Tap"];
}

-(void)threeFingerSingleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"31Tap"]:@"31Tap"];
}
-(void)threeFingerDoubleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"32Tap"]:@"32Tap"];
}
-(void)threeFingerTripleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"33Tap"]:@"33Tap"];
}
-(void)threeFingerQuadrupleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"34Tap"]:@"34Tap"];
}


-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event {
    int newFingers = [[event allTouches]count];
    // do not count down -- otherwise, two fingers will trigger off three if the dismount is not perfectly even!
    if (newFingers > _fingers) _fingers=newFingers;
    NSLog(@"currently %d fingers",_fingers);
}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    NSUInteger numTaps = [[touches anyObject] tapCount];
    int finalFingers = (int)[[event allTouches] count];
    if (_fingers==0) _fingers=finalFingers;
    NSLog(@"end %d fingers",_fingers);

    float delay = 0.3;
    switch (_fingers) {
        case 1:
            if (numTaps < 2)
            {
                // check single-tap position here vs on-screen regions
                UITouch *touch = [[event allTouches] anyObject];
                CGPoint location = [touch locationInView:touch.view];

                // set default button zones
                int checkTopZero = topMargin-20;
                int checkTop = topMargin+30;
                int checkLeftZero = leftMargin;
                int checkLeft = (self.view.bounds.size.width/3);
                int middleButtonLeft = (self.view.bounds.size.width/3);
                int middleButtonRight = (self.view.bounds.size.width/3)+(self.view.bounds.size.width/3);
            
                // set check zones for side-by-side
                if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]&(!UIInterfaceOrientationIsLandscape(_activeOrientation))) {
//                    checkTopZero = (self.view.bounds.size.height/2)+36;
//                    checkTop = (self.view.bounds.size.height/2)+76;
                    checkTopZero = topMargin-20;
                    checkTop = topMargin+30;
                }
                if ([[defaults objectForKey:@"ArtDisplayLayout"] isEqual:@"1"]&(UIInterfaceOrientationIsLandscape(_activeOrientation))) {
//                    checkLeftZero = (self.view.bounds.size.width/2)+36;
//                    checkLeft = (self.view.bounds.size.width/2)+136;
                    int activeWidth = self.view.bounds.size.width-leftMargin;
                    checkLeftZero = leftMargin;
                    checkLeft = leftMargin+(activeWidth/3);
                    middleButtonLeft = leftMargin+(activeWidth/3);
                    middleButtonRight = leftMargin+(activeWidth/3)+(activeWidth/3);
                }
                
                // check for button zones
                if (location.y<checkTop&location.y>checkTopZero) { // top bar region
                    if (location.x<checkLeft&location.x>checkLeftZero) { if (![[defaults objectForKey:@"TopLeft"] isEqual:@"Unassigned"]) { [self performPlayerAction:[defaults objectForKey:@"TopLeft"] :@"TopLeft"];} } // left button
                    else if (location.x > self.view.bounds.size.width-100) { if (![[defaults objectForKey:@"TopRight"] isEqual:@"Unassigned"]) { [self performPlayerAction:[defaults objectForKey:@"TopRight"] :@"TopRight"];}  } // right button
                    else if (location.x<middleButtonRight&location.x>middleButtonLeft){ if (![[defaults objectForKey:@"TopCenter"] isEqual:@"Unassigned"]) { [self performPlayerAction:[defaults objectForKey:@"TopCenter"] :@"TopCenter"];}  } // center button
                }
                else if (location.y>self.view.bounds.size.height-40-[self getBannerHeight]) { // bottom bar region
                    if (location.x<checkLeft&location.x>checkLeftZero) { if (![[defaults objectForKey:@"BottomLeft"] isEqual:@"Unassigned"]) { [self performPlayerAction:[defaults objectForKey:@"BottomLeft"] :@"BottomLeft"];} } // left button
                    else if (location.x > self.view.bounds.size.width-100) { if (![[defaults objectForKey:@"BottomRight"] isEqual:@"Unassigned"]) { [self performPlayerAction:[defaults objectForKey:@"BottomRight"] :@"BottomRight"];}  } // right button
                    else if (location.x<middleButtonRight&location.x>middleButtonLeft) { if (![[defaults objectForKey:@"BottomCenter"] isEqual:@"Unassigned"]) { [self performPlayerAction:[defaults objectForKey:@"BottomCenter"] :@"BottomCenter"];}  } // center button
                }
                else {
                    [self performSelector:@selector(oneFingerSingleTap) withObject:nil afterDelay:delay ];
                    [self.nextResponder touchesEnded:touches withEvent:event];
                }
            }
            else if(numTaps == 2)
            {
                [NSObject cancelPreviousPerformRequestsWithTarget:self];
                [self performSelector:@selector(oneFingerDoubleTap) withObject:nil afterDelay:delay ];
            }
            else if(numTaps == 3)
            {
                [NSObject cancelPreviousPerformRequestsWithTarget:self];
                [self performSelector:@selector(oneFingerTripleTap) withObject:nil afterDelay:delay ];
            }
            else if(numTaps == 4)
            {
                [NSObject cancelPreviousPerformRequestsWithTarget:self];
                [self performSelector:@selector(oneFingerQuadrupleTap) withObject:nil afterDelay:delay ];
            }
            break;
            
        case 2:
            if (numTaps < 2)
            {
                [self performSelector:@selector(twoFingerSingleTap) withObject:nil afterDelay:delay ];
                [self.nextResponder touchesEnded:touches withEvent:event];
            }
            else if(numTaps == 2)
            {
                [NSObject cancelPreviousPerformRequestsWithTarget:self];
                [self performSelector:@selector(twoFingerDoubleTap) withObject:nil afterDelay:delay ];
            }
            else if(numTaps == 3)
            {
                [NSObject cancelPreviousPerformRequestsWithTarget:self];
                [self performSelector:@selector(twoFingerTripleTap) withObject:nil afterDelay:delay ];
            }
            else if(numTaps == 4)
            {
                [NSObject cancelPreviousPerformRequestsWithTarget:self];
                [self performSelector:@selector(twoFingerQuadrupleTap) withObject:nil afterDelay:delay ];
            }
            break;
            
        case 3:
            if (numTaps < 2)
            {
                [self performSelector:@selector(threeFingerSingleTap) withObject:nil afterDelay:delay ];
                [self.nextResponder touchesEnded:touches withEvent:event];
            }
            else if(numTaps == 2)
            {
                [NSObject cancelPreviousPerformRequestsWithTarget:self];
                [self performSelector:@selector(threeFingerDoubleTap) withObject:nil afterDelay:delay ];
            }
            else if(numTaps == 3)
            {
                [NSObject cancelPreviousPerformRequestsWithTarget:self];
                [self performSelector:@selector(threeFingerTripleTap) withObject:nil afterDelay:delay ];
            }
            else if(numTaps == 4)
            {
                [NSObject cancelPreviousPerformRequestsWithTarget:self];
                [self performSelector:@selector(threeFingerQuadrupleTap) withObject:nil afterDelay:delay ];
            }
           break;
            
        default:
            break;
            
    }
    _fingers=0;
}


/****** Gesture Actions end *********************************************************************************************************************************/
/****** Player Actions begin *********************************************************************************************************************************/

- (void)performPlayerAction:(NSString *)action :(NSString*)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    [self drawActionHUD:action];
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];

    NSLog(@"Performing action %@",action);

    if ([action isEqual:@"Unassigned"]) NSLog(@"%@ sent unassigned command",sender);
    else if ([action isEqual:@"Menu"]) { [self scrubTimerKiller]; if ([[defaults objectForKey:@"disableAdBanners"] isEqual:@"NO"]) [self killAdBanner]; [self performSegueWithIdentifier: @"goToSettings" sender: self]; }
    else if ([action isEqual:@"PlayPause"]) [self togglePlayPause];
    else if ([action isEqual:@"Play"]) [self playOrDefault];
    else if ([action isEqual:@"Pause"]) [mediaPlayer pause];
    else if ([action isEqual:@"Next"]) { [self next]; }
    else if ([action isEqual:@"Previous"]) { [self previous]; }
    else if ([action isEqual:@"RestartPrevious"]) { [self restartPrevious]; }
    else if ([action isEqual:@"Restart"]) { [mediaPlayer skipToBeginning]; }
    else if ([action isEqual:@"Rewind"]) [self rewind];
    else if ([action isEqual:@"FastForward"]) [self fastForward];
    else if ([action isEqual:@"VolumeUp"]) [self increaseVolume];
    else if ([action isEqual:@"VolumeDown"]) [self decreaseVolume];
    else if ([action isEqual:@"StartDefaultPlaylist"]) [self playDefaultPlaylist];
    else if ([action isEqual:@"SongPicker"]) [self showSongPicker];
    else if ([action isEqual:@"PlayCurrentArtist"]) [self playCurrentArtist];
    else if ([action isEqual:@"PlayCurrentAlbum"]) [self playCurrentAlbum];
    else if ([action isEqual:@"ToggleShuffle"]) [self toggleShuffle];
    else if ([action isEqual:@"ToggleRepeat"]) [self toggleRepeat];
    else if ([action isEqual:@"DecreaseRating"]) [self decreaseRating];
    else if ([action isEqual:@"IncreaseRating"]) [self increaseRating];
    else if ([action isEqual:@"ShowQuickStart"]) [self showInstructions];
    else if ([action isEqual:@"NavigateHome"]) [self navigateHome];
    else if ([action isEqual:@"NavigateToWork"]) [self navigateToWork];
    else if ([action isEqual:@"NavigateToContact"]) [self pickContactAddress];
}

-(void) toggleShuffle {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ([[defaults objectForKey:@"shuffle"] isEqual:@"YES"]) {
        mediaPlayer.shuffleMode = MPMusicShuffleModeOff;
        [defaults setObject:@"NO" forKey:@"shuffle"];
    } else {
        mediaPlayer.shuffleMode = MPMusicShuffleModeSongs;
        [defaults setObject:@"YES" forKey:@"shuffle"];
    }
}

-(void) toggleRepeat {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ([[defaults objectForKey:@"repeat"] isEqual:@"YES"]) {
        mediaPlayer.repeatMode = MPMusicRepeatModeNone;
        [defaults setObject:@"NO" forKey:@"repeat"];
    } else {
        mediaPlayer.repeatMode = MPMusicRepeatModeAll;
        [defaults setObject:@"YES" forKey:@"repeat"];
    }
}

-(void) next {
    [self scrollingTimerKiller];
    [mediaPlayer skipToNextItem];
}

-(void) previous {
    [self scrollingTimerKiller];
    [mediaPlayer skipToPreviousItem];
}

-(void) fastForward {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [mediaPlayer setCurrentPlaybackTime:[mediaPlayer currentPlaybackTime]+[[defaults objectForKey:@"seekSensitivity"] floatValue]];
}

-(void)rewind {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [mediaPlayer setCurrentPlaybackTime:[mediaPlayer currentPlaybackTime]-[[defaults objectForKey:@"seekSensitivity"] floatValue]];
}

- (void) increaseVolume {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    float volume = mediaPlayer.volume;
    mediaPlayer.volume = volume+[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    _volumeBase = _volumeBase+[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    _volumeTarget = _volumeTarget+[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    if (_volumeBase>1) _volumeBase=1;
    if (_volumeTarget>1) _volumeTarget=1;
    _volumeTenth = _volumeBase/100;
    [self setupHUD];
}

- (void) decreaseVolume {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    float volume = mediaPlayer.volume;
    mediaPlayer.volume = volume-[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    _volumeBase = _volumeBase-[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    _volumeTarget = _volumeTarget-[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    if (_volumeBase<0) _volumeBase=0;
    if (_volumeTarget<0) _volumeTarget=0;
    _volumeTenth = _volumeBase/100;
    [self setupHUD];
}

- (void) playOrDefault {
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) [self playDefaultPlaylist];
    else [mediaPlayer play];
}

- (void) togglePlayPause {
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) [self playOrDefault];
    else if([mediaPlayer playbackState]==MPMusicPlaybackStatePlaying) {
        [mediaPlayer pause];
    } else {
        [mediaPlayer play];
    }
}

- (void) restartPrevious {
    if ([mediaPlayer currentPlaybackTime] < 2.5f) [self previous]; else [mediaPlayer skipToBeginning];
}

/*** timer functionality ********************************************************************************************************************************************/


-(void)scrollingTimerKiller {
    [self marqueeTimerKiller];
    _marqueePosition=0;
    if ( [[self scrollingTimer] isValid]){
        [[self scrollingTimer] invalidate];
        _timersRunning--;
    }
}

-(void)marqueeTimerKiller {
    if ( [[self marqueeTimer] isValid]){
        [[self marqueeTimer] invalidate];
    }
}

-(void)scrollSongTitle:(id)parameter{
    float textWidth = [_songTitle.text sizeWithFont:_songTitle.font].width;
    _marqueePosition=_marqueePosition+2;
    _adjustedSongFontSize = _songTitle.font.pointSize;
    _songTitle.frame=CGRectMake(leftMargin-(_marqueePosition), _songTitle.frame.origin.y, textWidth, _songTitle.frame.size.height);
    if (_marqueePosition >= textWidth) {
        [self scrollingTimerKiller];
        [self marqueeTimerKiller];
        _songTitle.frame=CGRectMake(leftMargin, _songTitle.frame.origin.y, textWidth, _songTitle.frame.size.height);
        _songTitle.baselineAdjustment = UIBaselineAdjustmentAlignCenters;
        [self startMarqueeTimer];
    }
}

// this super kludgey fix gives the labels a moment to set up before startTimer grabs the width
- (void)firstStartTimer {
    self.marqueeTimer = [NSTimer scheduledTimerWithTimeInterval: 0.5f
                                                  target: self
                                                selector: @selector(startMarqueeTimer)
                                                userInfo: nil
                                                 repeats: NO];
}

- (void)startMarqueeTimer { // wait 4 seconds on title, then scroll it
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        if ([[defaults objectForKey:@"TitleScrollLong"] isEqual:@"YES"]) self.marqueeTimer = [NSTimer scheduledTimerWithTimeInterval: 4
                                                      target: self
                                                    selector: @selector(startScrollingTimer)
                                                    userInfo: nil
                                                     repeats: NO];
    // otherwise the timer will restart itself to check for need due to new orientations
}

- (void)startScrollingTimer {
    float textWidth = [_songTitle.text sizeWithFont:_songTitle.font].width;
    if ((_timersRunning==0) & (textWidth > self.view.bounds.size.width)) {
        [self scrollingTimerKiller];
        self.scrollingTimer = [NSTimer scheduledTimerWithTimeInterval:   0.025f
                                                    target: self
                                                  selector: @selector(scrollSongTitle:)
                                                  userInfo: nil
                                                   repeats: YES];
        _timersRunning++;
        //NSLog(@"%d timers running", _timersRunning);
    }
}

- (void)playDefaultPlaylist {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    
    NSLog(@"Playing %@",[defaults objectForKey:@"playlist"]);
    
    if ([[defaults objectForKey:@"playlist"] isEqual:@"All Songs, Shuffled"]) [self playAllSongs];
    else if ([[defaults objectForKey:@"playlist"] isEqual:@"All Songs by Album"]) [self playAllByAlbum];
    else [self playConcretePlaylist];
}

- (void)playAllByAlbum {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    mediaPlayer.shuffleMode = MPMusicShuffleModeAlbums;
    [defaults setObject:@"YES" forKey:@"shuffle"];
    MPMediaQuery* query = [MPMediaQuery songsQuery];
    [query setGroupingType:MPMediaGroupingAlbum];
    [mediaPlayer setQueueWithQuery:query];
    [mediaPlayer play];
    [defaults synchronize];
}


-(void) playCurrentArtist {
    
    //Create a query that will return all songs by The Beatles grouped by album
    //    [query addFilterPredicate:[MPMediaPropertyPredicate predicateWithValue:@"The Beatles" forProperty:MPMediaItemPropertyArtist comparisonType:MPMediaPredicateComparisonEqualTo]];
    //    [query setGroupingType:MPMediaGroupingAlbum];
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    mediaPlayer.shuffleMode = MPMusicShuffleModeAlbums;
    [defaults setObject:@"YES" forKey:@"shuffle"];
    MPMediaQuery* query = [MPMediaQuery songsQuery];
    NSString *currentArtist = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtist];
    [query addFilterPredicate:[MPMediaPropertyPredicate predicateWithValue:currentArtist forProperty:MPMediaItemPropertyArtist comparisonType:MPMediaPredicateComparisonEqualTo]];
    [query setGroupingType:MPMediaGroupingAlbum];
    [mediaPlayer setQueueWithQuery:query];
    [mediaPlayer play];
    [defaults synchronize];
}

-(void) playCurrentAlbum {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    mediaPlayer.shuffleMode = MPMusicShuffleModeOff;
    [defaults setObject:@"NO" forKey:@"shuffle"];
    MPMediaQuery* query = [MPMediaQuery songsQuery];
    [query addFilterPredicate:[MPMediaPropertyPredicate predicateWithValue:[mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyAlbumTitle] forProperty:MPMediaItemPropertyAlbumTitle comparisonType:MPMediaPredicateComparisonEqualTo]];
    [query setGroupingType:MPMediaGroupingAlbum];
    [mediaPlayer setQueueWithQuery:query];
    [mediaPlayer play];
    [defaults synchronize];
}

- (void)playAllSongs {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    MPMediaQuery* query = [MPMediaQuery songsQuery];
    mediaPlayer.shuffleMode = MPMusicShuffleModeSongs;
    [defaults setObject:@"YES" forKey:@"shuffle"];
    [mediaPlayer setQueueWithQuery:query];
    [mediaPlayer play];
    [defaults synchronize];
}

-(void)playConcretePlaylist {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    MPMediaQuery *query = [MPMediaQuery playlistsQuery];
    _playlists = [query collections];

    NSString *selectedPlaylist = [defaults objectForKey:@"playlist"];

    MPMediaPlaylist *thePlaylist;
    for(int i = 0; i < [_playlists count]; i++)
    {
        if ([[[_playlists objectAtIndex:i] valueForProperty: MPMediaPlaylistPropertyName] isEqual:selectedPlaylist]) thePlaylist = [_playlists objectAtIndex:i];
    }

    [mediaPlayer setQueueWithItemCollection:thePlaylist];
    
//    mediaPlayer.nowPlayingItem = [thePlaylist.items objectAtIndex:0];
    [mediaPlayer play];
}

-(void)showSongPicker {
    MPMediaPickerController *mediaPicker = [[MPMediaPickerController alloc] initWithMediaTypes: MPMediaTypeAny];
    [self scrubTimerKiller];
    
    mediaPicker.delegate = self;
    mediaPicker.allowsPickingMultipleItems = YES;
    mediaPicker.prompt = @"Select songs to play";
    [self presentViewController:mediaPicker animated:YES completion:nil];
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
}

- (void) mediaPicker: (MPMediaPickerController *) mediaPicker didPickMediaItems: (MPMediaItemCollection *) mediaItemCollection
{
    
    if (mediaItemCollection) {
        [mediaPlayer setQueueWithItemCollection: mediaItemCollection];
        [mediaPlayer play];
    }
    [self dismissViewControllerAnimated: YES completion:nil];
}

- (void) mediaPickerDidCancel: (MPMediaPickerController *) mediaPicker
{
    [self dismissViewControllerAnimated: YES completion:nil];
}

// it is alleged in some places this information is read-only
-(void) setRating:(int)newRating {
    MPMediaItem *song = [mediaPlayer nowPlayingItem];
    [song setValue:[NSNumber numberWithInteger:newRating] forKey:@"rating"];
}

-(void) increaseRating {
    MPMediaItem *song = [mediaPlayer nowPlayingItem];
    int newRating = (int)[[song valueForKey:@"rating"] floatValue];
    if (newRating<5) newRating++;
    [song setValue:[NSNumber numberWithInteger:newRating] forKey:@"rating"];
}

-(void) decreaseRating {
    MPMediaItem *song = [mediaPlayer nowPlayingItem];
    int newRating = (int)[[song valueForKey:@"rating"] floatValue];
    if (newRating>0) newRating--;
    [song setValue:[NSNumber numberWithInteger:newRating] forKey:@"rating"];
}

- (void)addressSearch:(NSString *)address {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:address forKey:@"lastDestination"];
    [defaults synchronize];
    CLGeocoder *geocoder = [[CLGeocoder alloc] init];
    NSLog(@"navigating to %@",address);
    [geocoder geocodeAddressString:address completionHandler:^(NSArray *placemarks, NSError *error) {
        if (error) {
            NSLog(@"%@", error);
        } else {
            thePlacemark = [placemarks lastObject];
/*            float spanX = 1.00725;
            float spanY = 1.00725;
            MKCoordinateRegion region;
            region.center.latitude = thePlacemark.location.coordinate.latitude;
            region.center.longitude = thePlacemark.location.coordinate.longitude;
            region.span = MKCoordinateSpanMake(spanX, spanY);
            [_map setRegion:region animated:YES];
 */
            [self addAnnotation:thePlacemark];
            [self drawRoute];
        }
    }];
}

- (void)addAnnotation:(CLPlacemark *)placemark {
    MKPointAnnotation *point = [[MKPointAnnotation alloc] init];
    point.coordinate = CLLocationCoordinate2DMake(placemark.location.coordinate.latitude, placemark.location.coordinate.longitude);
    point.title = [placemark.addressDictionary objectForKey:@"Street"];
    point.subtitle = [placemark.addressDictionary objectForKey:@"City"];
    [_map addAnnotation:point];
}

-(MKAnnotationView *)mapView:(MKMapView *)mapView viewForAnnotation:(id<MKAnnotation>)annotation {
    // If it's the user location, just return nil.
    if ([annotation isKindOfClass:[MKUserLocation class]])
        return nil;
    // Handle any custom annotations.
    if ([annotation isKindOfClass:[MKPointAnnotation class]]) {
        // Try to dequeue an existing pin view first.
        MKPinAnnotationView *pinView = (MKPinAnnotationView*)[_map dequeueReusableAnnotationViewWithIdentifier:@"CustomPinAnnotationView"];
        if (!pinView)
        {
            // If an existing pin view was not available, create one.
            pinView = [[MKPinAnnotationView alloc] initWithAnnotation:annotation reuseIdentifier:@"CustomPinAnnotationView"];
            pinView.canShowCallout = YES;
        } else {
            pinView.annotation = annotation;
        }
        return pinView;
    }
    return nil;
}

- (IBAction)drawRoute {
    [_map removeOverlay:routeDetails.polyline];

    MKDirectionsRequest *directionsRequest = [[MKDirectionsRequest alloc] init];
    MKPlacemark *placemark = [[MKPlacemark alloc] initWithPlacemark:thePlacemark];
    [directionsRequest setSource:[MKMapItem mapItemForCurrentLocation]];
    [directionsRequest setDestination:[[MKMapItem alloc] initWithPlacemark:placemark]];
    directionsRequest.transportType = MKDirectionsTransportTypeAutomobile;
    MKDirections *directions = [[MKDirections alloc] initWithRequest:directionsRequest];
    [directions calculateDirectionsWithCompletionHandler:^(MKDirectionsResponse *response, NSError *error) {
        if (error) {
            NSLog(@"Error %@", error.description);
        } else {
            routeDetails = response.routes.lastObject;
            [_map addOverlay:routeDetails.polyline];
            NSLog(@"Destination %@",[placemark.addressDictionary objectForKey:@"Street"]);
            NSLog(@"Distance %@",[NSString stringWithFormat:@"%0.1f Miles", routeDetails.distance/1609.344]);
            for (int i = 0; i < routeDetails.steps.count; i++) {
                MKRouteStep *step = [routeDetails.steps objectAtIndex:i];
                NSString *newStep = step.instructions;
                NSLog(@"Route %@",newStep);
            }
        }
    }];
}

-(MKOverlayRenderer *)mapView:(MKMapView *)mapView rendererForOverlay:(id<MKOverlay>)overlay {
    MKPolylineRenderer  * routeLineRenderer = [[MKPolylineRenderer alloc] initWithPolyline:routeDetails.polyline];
    routeLineRenderer.strokeColor = [UIColor redColor];
    routeLineRenderer.lineWidth = 5;
    return routeLineRenderer;
}

- (void)clearRoute {
    [_map removeOverlay:routeDetails.polyline];
}

- (void) navigateHome {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self addressSearch:[defaults objectForKey:@"homeAddress"]];
}


- (void) navigateToWork {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self addressSearch:[defaults objectForKey:@"workAddress"]];
}

- (void) pickContactAddress {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self scrubTimerKiller];
    [self performSegueWithIdentifier: @"openContacts" sender: self];
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    settingsTableViewController *destination = [segue destinationViewController];
    
    // set up passthrough for subsequent view controller
    NSMutableDictionary *passthrough = [NSMutableDictionary dictionary];
    
    //preserve data from higher menus in passthrough dictionary
    [passthrough setObject:@"openContacts" forKey:@"sender"];
    destination.passthrough = passthrough;
}

@end