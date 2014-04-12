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
int bottomMargin = 20;
int albumTitleY = 0;
int songTitleY = 0;

@interface ttunesViewController ()
@property UIView *lineView,*playbackLineView,*edgeViewBG,*playbackEdgeViewBG,*nightTimeFade,*bgView;
@property UILabel *actionHUD;
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

- (void)deviceOrientationDidChangeNotification:(NSNotification*)note
{
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
    
    _albumArt.frame = CGRectMake(0,0, self.view.bounds.size.width,self.view.bounds.size.height);
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

- (id)init{
//    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
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

    [self setupHUD];
    
/*    NSLog(@"*** gps moved ***");
    NSLog(@"base volume:%f",_volumeBase);
    NSLog(@"real volume:%f",mediaPlayer.volume);
    NSLog(@"target volume:%f",_volumeTarget);
    NSLog(@"Speed %f is %f mph", newLocation.speed,newLocation.speed*2.23694);
*/
}

- (int)getBannerHeight:(UIDeviceOrientation)orientation {
    if (!self.bannerIsVisible) return 0;
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
    if (adBanner!=NULL) [adBanner removeFromSuperview];
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
    
    if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"2"]) bottomMargin = 20; else bottomMargin = 0;
    
    if ([[defaults objectForKey:@"GPSVolume"] isEqual:@"YES"]) [self startGPSVolume];
    
    [self.navigationController setNavigationBarHidden:YES];
    if ([[defaults objectForKey:@"disableAdBanners"] isEqual:@"YES"]) [self killAdBanner];
    if ([[defaults objectForKey:@"disableAdBanners"] isEqual:@"NO"]) [self initAdBanner];
    
    
    NSLog(@"ads are disabled? %@",[defaults objectForKey:@"disableAdBanners"]);
    
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
    
//    [self initAdBanner];
}

- (void)viewDidLoad
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [super viewDidLoad];
 
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

    [self.view addSubview:_bgView];
    [self.view addSubview:_albumArt];
    [self.view addSubview:_edgeViewBG];
    [self.view addSubview:_playbackEdgeViewBG];
    [self.view addSubview:_lineView];
    [self.view addSubview:_playbackLineView];
    [self.view bringSubviewToFront:_artistTitle];
    [self.view bringSubviewToFront:_songTitle];
    [self.view bringSubviewToFront:_albumTitle];
    [self.view addSubview:_actionHUD];
    [self.view addSubview:_nightTimeFade];

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
    
    [self initAdBanner];
    _speedTier = 0;
}

- (void) initAdBanner {
    if (!adBanner) {
        adBanner = [[ADBannerView alloc] initWithFrame:CGRectMake(0,self.view.bounds.size.height,self.view.bounds.size.width,[self getBannerHeight])];
        self.bannerIsVisible = NO;
        adBanner.delegate = self;
        [self.view addSubview:adBanner];
    }
    if (![adBanner isDescendantOfView:self]) {
        NSLog(@"adding banner view");
        [self.view addSubview:adBanner];
    }
}
/*
- (void) orientationChanged:(NSNotification *)note{
//    _nightTimeFade.frame=CGRectMake(0, 0, self.view.bounds.size.width, self.view.bounds.size.height);
    UIDevice * device = [UIDevice currentDevice];
    switch(device.orientation)
    {
        case UIDeviceOrientationPortrait:
            
            break;
        case UIDeviceOrientationPortraitUpsideDown:
            
            break;
        case UIDeviceOrientationLandscapeLeft:
            
            break;
        case UIDeviceOrientationLandscapeRight:
            
            break;
            
        default:
            break;
    };
}
*/
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

    [self setGlobalColors];
    [self scrollingTimerKiller]; [self startMarqueeTimer];
    [self setupLabels];
    [self setupHUD];
    MPMediaItem *song = [mediaPlayer nowPlayingItem];
    if (song) {
        NSString *title = [song valueForProperty:MPMediaItemPropertyTitle];
        NSString *album = [song valueForProperty:MPMediaItemPropertyAlbumTitle];
        NSString *artist = [song valueForProperty:MPMediaItemPropertyArtist];
        NSString *playCount = [song valueForProperty:MPMediaItemPropertyPlayCount];
        
        NSLog(@"title: %@", title);
        NSLog(@"album: %@", album);
        NSLog(@"artist: %@", artist);
        NSLog(@"playCount: %@", playCount);
    }
}

- (void) setGlobalColors {
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
    
    MPMediaItemArtwork *artwork = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtwork];
    if ((artwork != nil) & ([[defaults objectForKey:@"showAlbumArt"] isEqual:@"YES"])) {
        // so do a second check to see if it has at least 50x50 pixels
        if ([artwork imageWithSize:CGSizeMake(50,50)]) {
            _albumArt.image = [artwork imageWithSize:CGSizeMake(self.view.bounds.size.width,self.view.bounds.size.height)];
            _albumArt.alpha = 0.25f;
            _albumArt.contentMode = UIViewContentModeCenter;
            //                _albumArt.contentMode = UIViewContentModeCenter;
            
            if ([[defaults objectForKey:@"albumArtColors"] isEqual:@"YES"]) {
                LEColorPicker *colorPicker = [[LEColorPicker alloc] init];
                LEColorScheme *colorScheme = [colorPicker colorSchemeFromImage:[artwork imageWithSize:CGSizeMake(40,40)]];
                
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
                
/*
                  _bgView.backgroundColor = [colorScheme backgroundColor];
                _artistTitle.textColor = [colorScheme primaryTextColor];
                _songTitle.textColor = [UIColor colorWithRed: (pred+sred)/2   green: (pgreen+sgreen)/2   blue:(pblue+sblue)/2   alpha:1];
                _albumTitle.textColor = [colorScheme secondaryTextColor];
 */
                _themeBG = [colorScheme backgroundColor];
                _themeColorArtist = [colorScheme primaryTextColor];
                _themeColorSong = [UIColor colorWithRed: (sred+(pred*3))/4   green: (sgreen+(pgreen*3))/4   blue:(sblue+(pblue*3))/4   alpha:1];
                _themeColorAlbum = [colorScheme secondaryTextColor];
            }
        } else _albumArt.alpha = 0.0f;
        
    } else _albumArt.alpha = 0.0f;
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
//    else if([mediaPlayer playbackState]==MPMusicPlaybackStatePlaying) {

    float red, green, blue, alpha;
    float red2, green2, blue2, alpha2;

    [_themeColorArtist getRed:&red green:&green blue:&blue alpha:&alpha];
    [_themeBG getRed:&red2 green:&green2 blue:&blue2 alpha:&alpha2];

//    MPMediaItem *playingItem=[mediaPlayer nowPlayingItem];
    long totalPlaybackTime = [[[mediaPlayer nowPlayingItem] valueForProperty: @"playbackDuration"] longValue];

    float playbackPosition=(self.view.bounds.size.width*([mediaPlayer currentPlaybackTime]/totalPlaybackTime));

    
    
//    NSLog(@"%f of %ld yields %f",[mediaPlayer currentPlaybackTime],totalPlaybackTime,playbackPosition);
    _playbackLineView.backgroundColor = [UIColor clearColor];
    _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
        
        long height;
        height = self.view.bounds.size.height-[self getBannerHeight];
/*
        if ([[defaults objectForKey:@"disableAdBanners"] isEqual:@"YES"]) height = self.view.bounds.size.height;
        else if (self.view.bounds.size.height==320) height = self.view.bounds.size.height-32; //reduce height for landscape ad banner
        else height=self.view.bounds.size.height-50; // reduce height for portrait iAd banner
 */
        
    if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"0"]) {
        _playbackLineView.frame=CGRectMake(playbackPosition, 0,  self.view.bounds.size.width, self.view.bounds.size.height);
        _playbackLineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.35f];
        _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
    } else if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"1"]) {
        _playbackLineView.frame = CGRectMake(playbackPosition, 0, 15, self.view.bounds.size.height);
        _playbackLineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.35f];
        _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
    } else if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"2"]) {
        _playbackLineView.frame = CGRectMake(playbackPosition, height-15, 15, 80);
        _playbackLineView.backgroundColor = [UIColor colorWithRed:red2 green:green2 blue:blue2 alpha:1.f];
        _playbackEdgeViewBG.frame = CGRectMake(0, height-15, self.view.bounds.size.width, 80);
        _playbackEdgeViewBG.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.5f];
    }
    }
    
    // since this runs 5 times a second, update volume per GPS here
    mediaPlayer.volume=_volumeTarget;
//    if (mediaPlayer.volume < _volumeTarget) mediaPlayer.volume=mediaPlayer.volume+[[defaults objectForKey:@"volumeSensitivity"] floatValue];
//    else if (mediaPlayer.volume > _volumeTarget) mediaPlayer.volume=_volumeTarget; //  mediaPlayer.volume-[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    
    [self setupLabels];
}

- (void)setupLabels {
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
//    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    
    //    NSLog(@"Current theme should be %@",[defaults objectForKey:@"currentTheme"]);
    [self setGlobalColors];
    

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
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) { //output "nothing playing screen" if nothing playing
//    if ([mediaPlayer.nowPlayingItem]) { //output "nothing playing screen" if nothing playing
        _artistTitle.numberOfLines = 1;
        _artistTitle.text   = @"No music playing.";
        _artistTitle.font   = [UIFont systemFontOfSize:28];
        _artistTitle.textColor = _themeColorArtist;
        [_artistTitle setAlpha:0.8f];
        [_artistTitle sizeToFit];
        
        _songTitle.numberOfLines = 1;
        _songTitle.text   = @"Tap for default playlist.";
        _songTitle.font   = [UIFont systemFontOfSize:28];
        _songTitle.textColor = _themeColorSong;
        [_songTitle sizeToFit];
        _songTitle.frame = CGRectMake(leftMargin,(self.view.bounds.size.height/2)-[self getBannerHeight],self.view.bounds.size.width,30);
        
        _albumTitle.numberOfLines = 1;
        _albumTitle.text    = @"Long hold for menu.";
        _albumTitle.font    =  [UIFont systemFontOfSize:28];
        _albumTitle.textColor = _themeColorAlbum;
        [_albumTitle setAlpha:0.8f];
        _albumTitle.frame = CGRectMake(leftMargin,self.view.bounds.size.height-40-[self getBannerHeight],self.view.bounds.size.width,30);

    } else {
        _artistTitle.numberOfLines = 1;
        _artistTitle.text   = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtist];
        _artistTitle.font   = [UIFont systemFontOfSize:artistFontSize];
        _artistTitle.textColor = _themeColorArtist;
        [_artistTitle setAlpha:0.8f];
        _artistTitle.minimumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
        
        
        int songOffset = _songTitle.frame.origin.y;
        if (self.bannerIsVisible) songOffset=(self.view.bounds.size.height/2)-(_songTitle.frame.size.height/2);//-([self getBannerHeight]/2);
        // do not replace song title label if the scrolling marquee is handling that right now
        if (_timersRunning==0) {
            _songTitle.frame=CGRectMake(20-_marqueePosition, songOffset, self.view.bounds.size.width-rightMargin*2, _songTitle.frame.size.height);
            _songTitle.numberOfLines = 1;
            _songTitle.text   = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle];
            _songTitle.font   = [UIFont systemFontOfSize:songFontSize];
            _songTitle.textColor = _themeColorSong;
            _songTitle.minimumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
        }
        
        int albumOffset = _albumTitle.frame.origin.y;
        if (self.bannerIsVisible) albumOffset=(self.view.bounds.size.height-_albumTitle.frame.size.height)-[self getBannerHeight]-bottomMargin; //20 is bottom margin
        _albumTitle.frame=CGRectMake(leftMargin,albumOffset,self.view.bounds.size.width-rightMargin*2,_albumTitle.frame.size.height);
        _albumTitle.numberOfLines = 1;
        _albumTitle.font    = [UIFont systemFontOfSize:albumFontSize];
        _albumTitle.text    = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyAlbumTitle];
        _albumTitle.textColor = _themeColorAlbum;
        [_albumTitle setAlpha:0.8f];
        _albumTitle.minimumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
//        if (self.bannerIsVisible) _albumTitle.frame = CGRectOffset(_albumTitle.frame, 0, -[self getBannerHeight]);
    }
    if ([[defaults objectForKey:@"titleShrinkLong"] isEqual:@"YES"]) [self drawFittedText];
}

-(void)drawFittedText {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    int minFontSize = (int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
    
    // NSString class method: boundingRectWithSize:options:attributes:context is
    // available only on ios7.0 sdk.

    int songFontSize = (int)[[defaults objectForKey:@"artistFontSize"] floatValue];
    NSDictionary *attributes = @{NSFontAttributeName: [UIFont fontWithName:@"HelveticaNeue" size:songFontSize]};
    long textHeight = [[_songTitle font] fontWithSize:songFontSize];
	while( textHeight > minFontSize )
	{
        if ([_artistTitle.text sizeWithFont:[_artistTitle font]].width<self.view.bounds.size.width-(leftMargin+rightMargin)) break;
		[_artistTitle setFont:[[_artistTitle font] fontWithSize:--songFontSize]];
		textHeight = [_artistTitle.text sizeWithFont:[_artistTitle font]].height;
	}
 
    songFontSize = (int)[[defaults objectForKey:@"songFontSize"] floatValue];
    attributes = @{NSFontAttributeName: [UIFont fontWithName:@"HelveticaNeue" size:songFontSize]};
    textHeight = [[_songTitle font] fontWithSize:songFontSize];
	while( textHeight > minFontSize )
	{
        if ([_songTitle.text sizeWithFont:[_songTitle font]].width<self.view.bounds.size.width-(leftMargin+rightMargin)) break;
		[_songTitle setFont:[[_songTitle font] fontWithSize:--songFontSize]];
		textHeight = [_songTitle.text sizeWithFont:[_songTitle font]].height;
	}
    
    songFontSize = (int)[[defaults objectForKey:@"albumFontSize"] floatValue];
    attributes = @{NSFontAttributeName: [UIFont fontWithName:@"HelveticaNeue" size:songFontSize]};
    textHeight = [[_albumTitle font] fontWithSize:songFontSize];
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
    //    NSLog(@"fadeHUDTimerKiller");
    if ( [[self actionHUDFadeTimer] isValid]){
        [[self actionHUDFadeTimer] invalidate];
    }
}

-(void)fadeHUDTimerKiller {
//    NSLog(@"fadeHUDTimerKiller");
    if ( [[self fadeHUDTimer] isValid]){
        [[self fadeHUDTimer] invalidate];
    }
}

- (void) scrubTimerKiller {
     if ([self.scrubTimer isValid]) { [self.scrubTimer invalidate]; }
}

-(void) startActionHUDFadeTimer {
        [self fadeActionHUDTimerKiller];
    _fadeActionHUDAlpha = 0.75f; //CGColorGetAlpha(_actionHUD.backgroundColor.CGColor);
        
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
            _actionHUD.textColor = [[UIColor whiteColor] colorWithAlphaComponent:0.75f];
            _actionHUD.backgroundColor = [[UIColor blackColor] colorWithAlphaComponent:0.75f];
            _actionHUD.layer.cornerRadius = 10;
            _actionHUD.lineBreakMode = NSLineBreakByClipping;
            
            if ([action isEqual:@"Rewind"]) _actionHUD.text = @"\u2190"; //@"\u2190";
            else if ([action isEqual:@"FastForward"]) _actionHUD.text = @"\u2192";
            else if ([action isEqual:@"Play"]) _actionHUD.text = @"|>";
            else if ([action isEqual:@"Pause"]) _actionHUD.text = @"\u220e\u220e";
            else if ([action isEqual:@"PlayPause"]&(mediaPlayer.playbackState==MPMusicPlaybackStatePlaying)) _actionHUD.text = @"\u220e\u220e";
            else if ([action isEqual:@"PlayPause"]&(mediaPlayer.playbackState!=MPMusicPlaybackStatePlaying)) _actionHUD.text = @"\u25b8"; //@"\u2023"; //@"\u25ba";
            else if ([action isEqual:@"VolumeUp"]) _actionHUD.text = @"\u2191";
            else if ([action isEqual:@"VolumeDown"]) _actionHUD.text = @"\u2193";
            else if ([action isEqual:@"Next"]) _actionHUD.text = @"\u21c9";
            else if ([action isEqual:@"Previous"]|[action isEqual:@"RestartPrevious"]) _actionHUD.text = @"\u21c7";
            else if ([action isEqual:@"StartDefaultPlaylist"]|[action isEqual:@"PlayAllArtist"]|[action isEqual:@"PlayAlbum"]) _actionHUD.text = @"...";
            else _actionHUD.text = @"?";

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
/*    if ([[defaults objectForKey:@"disableBannerAds"] isEqual:@"YES"]) height = self.view.bounds.size.height;
    else if (self.view.bounds.size.height==320) height = self.view.bounds.size.height-32; //reduce height for landscape ad banner
    else height=self.view.bounds.size.height-50; // reduce height for portrait iAd banner
 */
    
    float volumeLevel=height-(height*mediaPlayer.volume);
    
    _lineView.backgroundColor = [UIColor clearColor];
    _edgeViewBG.backgroundColor = [UIColor clearColor];

    //setup for rectangle drawing display
    if ([[defaults objectForKey:@"HUDType"] isEqual:@"1"]) {
        _lineView.frame=CGRectMake(0, volumeLevel, self.view.bounds.size.width, self.view.bounds.size.height);
        _lineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.35f];
        _edgeViewBG.backgroundColor = [UIColor clearColor];
    } else if ([[defaults objectForKey:@"HUDType"] isEqual:@"2"]) {
        _lineView.frame = CGRectMake(0, volumeLevel, self.view.bounds.size.width, 15);
        _lineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.35f];
        _edgeViewBG.backgroundColor = [UIColor clearColor];
    } else if ([[defaults objectForKey:@"HUDType"] isEqual:@"3"]) {
        _lineView.frame = CGRectMake(self.view.bounds.size.width-15, volumeLevel, self.view.bounds.size.width, 15);
        _lineView.backgroundColor = [UIColor colorWithRed:red2 green:green2 blue:blue2 alpha:1.f];
        _edgeViewBG.frame = CGRectMake(self.view.bounds.size.width-15, 0, self.view.bounds.size.width, self.view.bounds.size.height);
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
                       self.DNRTimer = [NSTimer scheduledTimerWithTimeInterval: 0.2f
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
                       self.DNRTimer = [NSTimer scheduledTimerWithTimeInterval: 0.2f
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
                       self.DNRTimer = [NSTimer scheduledTimerWithTimeInterval: 0.2f
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
                       self.DNRTimer = [NSTimer scheduledTimerWithTimeInterval: 0.2f
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
    } // else, UIGestureRecognizerState[Changed / Ended]
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

//(unsigned long)gesture.numberOfTouches)
-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    NSUInteger numTaps = [[touches anyObject] tapCount];
    int finalFingers = [[event allTouches]count];
    if (_fingers==0) _fingers=finalFingers;
    NSLog(@"end %d fingers",_fingers);
    float delay = 0.3;
    switch (_fingers) {
        case 1:
            if (numTaps < 2)
            {
                [self performSelector:@selector(oneFingerSingleTap) withObject:nil afterDelay:delay ];
                [self.nextResponder touchesEnded:touches withEvent:event];
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
    [self setupLabels];
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
    _volumeTenth = _volumeBase/100;
    [self setupHUD];
}

- (void) decreaseVolume {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    float volume = mediaPlayer.volume;
    mediaPlayer.volume = volume-[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    _volumeBase = _volumeBase-[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    _volumeTarget = _volumeTarget-[[defaults objectForKey:@"volumeSensitivity"] floatValue];
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
//    NSLog(@"scrollingTimerKiller");
    [self marqueeTimerKiller];
    _marqueePosition=0;
    if ( [[self scrollingTimer] isValid]){
        [[self scrollingTimer] invalidate];
        _timersRunning--;
        //NSLog(@"%d timers running",_timersRunning);
    }
}

-(void)marqueeTimerKiller {
    [self setupLabels]; // also refresh labels every 4 seconds? keeps fading current
//    NSLog(@"marqueeTimerKiller");
    if ( [[self marqueeTimer] isValid]){
        [[self marqueeTimer] invalidate];
    }
}

// this always scrolls, even if the text fits.
-(void)scrollSongTitle:(id)parameter{
//    NSLog(@"%d timers running",_timersRunning);
    
    float textWidth = [_songTitle.text sizeWithFont:_songTitle.font].width;
    _marqueePosition=_marqueePosition+2;
    _adjustedSongFontSize = _songTitle.font.pointSize;
    _songTitle.frame=CGRectMake(20-(_marqueePosition), _songTitle.frame.origin.y, textWidth, _songTitle.frame.size.height);
//    NSLog(@"scrolling font size %f",_adjustedSongFontSize);
    
    if (_marqueePosition>= textWidth+20) {
        [self scrollingTimerKiller];
        [self marqueeTimerKiller];
        _songTitle.frame=CGRectMake(leftMargin, _songTitle.frame.origin.y, textWidth, _songTitle.frame.size.height);
        //_songTitle.adjustsFontSizeToFitWidth=YES;
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
    MPMediaQuery* query = [MPMediaQuery songsQuery];
    [defaults setObject:@"YES" forKey:@"shuffle"];
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


@end