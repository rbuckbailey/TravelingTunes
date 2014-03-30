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

@interface ttunesViewController ()
@property UIView *lineView,*playbackLineView,*edgeViewBG,*playbackEdgeViewBG;
@property int timersRunning;
@property float adjustedSongFontSize;
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
    //    UIDeviceOrientation orientation = [[UIDevice currentDevice] orientation];
    [self setupLabels];
    [self setupHUD];
}

- (IBAction)singleTapDetected:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"11Tap"]:@"11Tap"];
    NSLog(@"gesture is %@",[defaults objectForKey:@"11Tap"]);
}

- (id)init{
//    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    return 0;
}

- (void)viewWillAppear:(BOOL)animated
{
    [self.navigationController setNavigationBarHidden:YES];
    [self setupLabels];
    [self setupHUD];
    [self setupSystemHUD];
    //reset marquee
    //[self.timer invalidate]; //invalidating timer that isnt running causes crash?
    _marqueePosition=0; [self firstStartTimer];
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    _timersRunning=0;
    
    // attach to delegate so launch/exit actions can be called
    ttunesAppDelegate *appDelegate = [[UIApplication sharedApplication] delegate];
    appDelegate.ttunes = self;

    [self startPlaybackWatcher];
    _lineView = [[UIView alloc] init];
    _edgeViewBG = [[UIView alloc] init];
    _playbackEdgeViewBG = [[UIView alloc] init];

    _playbackLineView = [[UIView alloc] init];
    [self.view addSubview:_edgeViewBG];
    [self.view addSubview:_playbackEdgeViewBG];
    [self.view addSubview:_lineView];
    [self.view addSubview:_playbackLineView];
    _lineView.backgroundColor = [UIColor clearColor];
    _edgeViewBG.backgroundColor = [UIColor clearColor];
    _playbackLineView.backgroundColor = [UIColor clearColor];
    _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
    
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
    
    //notifier for orientation changes
    [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
    [[NSNotificationCenter defaultCenter]
     addObserver:self selector:@selector(orientationChanged:)
     name:UIDeviceOrientationDidChangeNotification
     object:[UIDevice currentDevice]];
}

- (void) orientationChanged:(NSNotification *)note{
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

-(void) startPlaybackWatcher {
    self.scrubTimer = [NSTimer scheduledTimerWithTimeInterval:   0.2f
                                                  target: self
                                                selector: @selector(updatePlaybackHUD)
                                                userInfo: nil
                                                 repeats: YES];
}


-(void) nowPlayingItemChanged:(NSNotification *)notification {
    MPMusicPlayerController *mediaPlayer = (MPMusicPlayerController *)notification.object;
    
    MPMediaItem *song = [mediaPlayer nowPlayingItem];
    
    if (song) {
        [self setupLabels];
        [self startMarqueeTimer];
        
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

/*** HUD display setups ********************************************************************************************************************************************/

-(void) updatePlaybackHUD {
    
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]!=NULL) {
//    else if([mediaPlayer playbackState]==MPMusicPlaybackStatePlaying) {

    
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSString *currentTheme = [defaults objectForKey:@"currentTheme"];
    NSMutableDictionary *themedict = [gestureController themes];
    NSArray *themecolors = [themedict objectForKey:currentTheme];
    UIColor *temp;
    UIColor *themebg = [themecolors objectAtIndex:0];
    UIColor *themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) {
        temp = themebg;
        themebg = themecolor;
        themecolor = temp;
    }
    float red, green, blue, alpha;
    float red2, green2, blue2, alpha2;

    [themecolor getRed:&red green:&green blue:&blue alpha:&alpha];
    [themebg getRed:&red2 green:&green2 blue:&blue2 alpha:&alpha2];

//    MPMediaItem *playingItem=[mediaPlayer nowPlayingItem];
    long totalPlaybackTime = [[[mediaPlayer nowPlayingItem] valueForProperty: @"playbackDuration"] longValue];

    float playbackPosition=(self.view.bounds.size.width*([mediaPlayer currentPlaybackTime]/totalPlaybackTime));

    
    
//    NSLog(@"%f of %ld yields %f",[mediaPlayer currentPlaybackTime],totalPlaybackTime,playbackPosition);
    _playbackLineView.backgroundColor = [UIColor clearColor];
    _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];

    if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"0"]) {
        _playbackLineView.frame=CGRectMake(playbackPosition, 0,  self.view.bounds.size.width, self.view.bounds.size.height);
        _playbackLineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.35f];
        _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
    } else if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"1"]) {
        _playbackLineView.frame = CGRectMake(playbackPosition, 0, 15, self.view.bounds.size.height);
        _playbackLineView.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.35f];
        _playbackEdgeViewBG.backgroundColor = [UIColor clearColor];
    } else if ([[defaults objectForKey:@"ScrubHUDType"] isEqual:@"2"]) {
        _playbackLineView.frame = CGRectMake(playbackPosition, self.view.bounds.size.height-15, 15, self.view.bounds.size.height);
        _playbackLineView.backgroundColor = [UIColor colorWithRed:red2 green:green2 blue:blue2 alpha:1.f];
        _playbackEdgeViewBG.frame = CGRectMake(0, self.view.bounds.size.height-15, self.view.bounds.size.width, 15);
        _playbackEdgeViewBG.backgroundColor = [UIColor colorWithRed:red green:green blue:blue alpha:0.5f];
    }
    }
}

- (void)setupLabels {
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    
    //    NSLog(@"Current theme should be %@",[defaults objectForKey:@"currentTheme"]);
    NSString *currentTheme = [defaults objectForKey:@"currentTheme"];
    NSMutableDictionary *themedict = [gestureController themes];
    NSArray *themecolors = [themedict objectForKey:currentTheme];
    UIColor *temp;
    UIColor *themebg = [themecolors objectAtIndex:0];
    UIColor *themecolor = [themecolors objectAtIndex:1];
    
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) {
        temp = themebg;
        themebg = themecolor;
        themecolor = temp;
    }
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
    //    NSLog(@"orientation is %d",orientation);
    //wtf is orientation 5??
    //    if (((orientation==1) | (orientation==2)) & [[defaults objectForKey:@"titleShrinkInPortrait"] isEqual:@"YES"]) {
    NSLog(@"height is %f",self.view.bounds.size.height);
    if (((self.view.bounds.size.height==480) | (self.view.bounds.size.height==568)) & [[defaults objectForKey:@"titleShrinkInPortrait"] isEqual:@"YES"]) {
        
        artistFontSize = (int)artistFontSize/2;
        if (artistFontSize<(int)[[defaults objectForKey:@"minimumFontSize"] floatValue]) artistFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
        songFontSize = (int)songFontSize/2;
        if (songFontSize<(int)[[defaults objectForKey:@"minimumFontSize"] floatValue]) songFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
        albumFontSize = (int)albumFontSize/2;
        if (albumFontSize<(int)[[defaults objectForKey:@"minimumFontSize"] floatValue]) albumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
    }
    //    NSLog(@"invert is %@",[defaults objectForKey:@"themeInvert"]);
    
    self.view.backgroundColor = themebg;
    
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) { //output "nothing playing screen" if nothing playing
        _artistTitle.numberOfLines = 1;
        _artistTitle.text   = @"No music playing.";
        _artistTitle.font   = [UIFont systemFontOfSize:28];
        _artistTitle.textColor = themecolor;
        
        [_artistTitle setAlpha:0.6f];
        [_artistTitle sizeToFit];
        
        _songTitle.numberOfLines = 1;
        _songTitle.text   = @"Tap for default playlist.";
        _songTitle.font   = [UIFont systemFontOfSize:28];
        _songTitle.textColor = themecolor;
        
        [_songTitle sizeToFit];
        
        _albumTitle.numberOfLines = 1;
        _albumTitle.text    = @"Long hold for menu.";
        _albumTitle.font    =  [UIFont systemFontOfSize:28];
        _albumTitle.textColor = themecolor;
        [_albumTitle setAlpha:0.6f];
    } else {
        _artistTitle.numberOfLines = 1;
        _artistTitle.text   = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtist];
        _artistTitle.font   = [UIFont systemFontOfSize:artistFontSize];
        _artistTitle.textColor = themecolor;
        [_artistTitle setAlpha:0.6f];
        _artistTitle.minimumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
        if ([[defaults objectForKey:@"titleShrinkLong"] isEqual:@"YES"]) _artistTitle.adjustsFontSizeToFitWidth=YES; else _artistTitle.adjustsFontSizeToFitWidth=NO;
        
        if (_timersRunning==0) {
            _songTitle.numberOfLines = 1;
            _songTitle.text   = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle];
            _songTitle.font   = [UIFont systemFontOfSize:songFontSize];
            _songTitle.textColor = themecolor;
            _songTitle.frame=CGRectMake(20-_marqueePosition, _songTitle.frame.origin.y, self.view.bounds.size.width, _songTitle.frame.size.height);
            _songTitle.minimumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
            _adjustedSongFontSize = _songTitle.font.pointSize;
            //NSLog(@"font size %f",_adjustedSongFontSize);

            if ([[defaults objectForKey:@"titleShrinkLong"] isEqual:@"YES"]) {
                _songTitle.adjustsFontSizeToFitWidth=YES;
                _adjustedSongFontSize = _songTitle.font.pointSize;
                //NSLog(@"adjusted font size %f",_adjustedSongFontSize);

            }
            else _songTitle.adjustsFontSizeToFitWidth=NO;
        }
        
        _albumTitle.numberOfLines = 1;
        _albumTitle.font    = [UIFont systemFontOfSize:albumFontSize];
        _albumTitle.text    = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyAlbumTitle];
        _albumTitle.textColor = themecolor;
        [_albumTitle setAlpha:0.6f];
        _albumTitle.minimumFontSize=(int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
        if ([[defaults objectForKey:@"titleShrinkLong"] isEqual:@"YES"]) _albumTitle.adjustsFontSizeToFitWidth=YES; else _albumTitle.adjustsFontSizeToFitWidth=NO;
    }
}

-(void) fadeHUD {
    _lineView.backgroundColor = [UIColor clearColor];
    _edgeViewBG.backgroundColor = [UIColor clearColor];
}

-(void) setupHUD {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSString *currentTheme = [defaults objectForKey:@"currentTheme"];
    NSMutableDictionary *themedict = [gestureController themes];
    NSArray *themecolors = [themedict objectForKey:currentTheme];
    UIColor *temp;
    UIColor *themebg = [themecolors objectAtIndex:0];
    UIColor *themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) {
        temp = themebg;
        themebg = themecolor;
        themecolor = temp;
    }
    float red, green, blue, alpha;
    float red2, green2, blue2, alpha2;

    [themecolor getRed:&red green:&green blue:&blue alpha:&alpha];
    [themebg getRed:&red2 green:&green2 blue:&blue2 alpha:&alpha2];

    float volumeLevel=self.view.bounds.size.height-(self.view.bounds.size.height*mediaPlayer.volume);
//    NSLog(@"volume is %f",mediaPlayer.volume);
    
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
/*
 self.timer = [NSTimer scheduledTimerWithTimeInterval: 2.5f
                                                  target: self
                                                selector: @selector(fadeHUD)
                                                userInfo: nil
                                                 repeats: NO];
 */
//    NSLog(@"Volume level is %f out of %f",volumeLevel,self.view.bounds.size.height);
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

    _direction = directionNone;
    
// this useful debug info is spammy
//    NSLog(@"%lu touches",(unsigned long)gesture.numberOfTouches);
    if (gesture.state == UIGestureRecognizerStateBegan)
    {
        _direction = directionNone;
    }
// this is continuous gesture, bad for some actions, good for others.
//    else if (gesture.state == UIGestureRecognizerStateChanged)
// this is trigger-once actions
    else if (gesture.state == UIGestureRecognizerStateChanged && _direction == directionNone)
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
                       self.marqueeTimer = [NSTimer scheduledTimerWithTimeInterval: 0.1f
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
                       self.marqueeTimer = [NSTimer scheduledTimerWithTimeInterval: 0.1f
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
               if ([[defaults objectForKey:keyContinuous] isEqual:@"YES"]) [self performPlayerAction:[defaults objectForKey:key]:key];
               else {
                   if (![[defaults objectForKey:@"doNotRepeat"] isEqual:key]) {
                       [self performPlayerAction:[defaults objectForKey:key]:key];
                       [defaults setObject:key forKey:@"doNotRepeat"];
                       self.marqueeTimer = [NSTimer scheduledTimerWithTimeInterval: 0.1f
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
               if ([[defaults objectForKey:keyContinuous] isEqual:@"YES"]) [self performPlayerAction:[defaults objectForKey:key]:key];
               else {
                   if (![[defaults objectForKey:@"doNotRepeat"] isEqual:key]) {
                       [self performPlayerAction:[defaults objectForKey:key]:key];
                       [defaults setObject:key forKey:@"doNotRepeat"];
                       self.marqueeTimer = [NSTimer scheduledTimerWithTimeInterval: 0.1f
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
        NSLog(@"Stop. Mediaplayer state is %ld",(long)[mediaPlayer playbackState]);
        //if ([mediaPlayer playbackState] == MPMusicPlaybackStateSeekingForward | [mediaPlayer playbackState]==MPMusicPlaybackStateSeekingBackward) [mediaPlayer endSeeking];
    }
}


- (void) resetDoNotRepeat {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:@"narf!" forKey:@"doNotRepeat"];
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
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"21Tap"]:@"21Tap"];
}

- (IBAction)threeFingerTap:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"31Tap"]:@"31Tap"];
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


/*
-(void)singleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"11Tap"]:@"11Tap"];
    NSLog(@"gesture is %@",[defaults objectForKey:@"11Tap"]);
}
-(void)doubleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"12Tap"]:@"12Tap"];
}
-(void)tripleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"13Tap"]:@"13Tap"];
}
-(void)quadrupleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"14Tap"]:@"14Tap"];
}


-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    NSUInteger numTaps = [[touches anyObject] tapCount];
    float delay = 0.2;
    if (numTaps < 2)
    {
        [self performSelector:@selector(singleTap) withObject:nil afterDelay:delay ];
        [self.nextResponder touchesEnded:touches withEvent:event];
    }
    else if(numTaps == 2)
    {
        [NSObject cancelPreviousPerformRequestsWithTarget:self];
        [self performSelector:@selector(doubleTap) withObject:nil afterDelay:delay ];
    }
    else if(numTaps == 3)
    {
        [NSObject cancelPreviousPerformRequestsWithTarget:self];
        [self performSelector:@selector(tripleTap) withObject:nil afterDelay:delay ];
    }
    else if(numTaps == 4)
    {
        [NSObject cancelPreviousPerformRequestsWithTarget:self];
        [self performSelector:@selector(quadrupleTap) withObject:nil afterDelay:delay ];
    }
}
*/

/****** Gesture Actions end *********************************************************************************************************************************/
/****** Player Actions begin *********************************************************************************************************************************/

- (void)performPlayerAction:(NSString *)action :(NSString*)sender {
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    NSLog(@"Performing action %@",action);
    if ([action isEqual:@"Unassigned"]) NSLog(@"%@ sent unassigned command",sender);
    else if ([action isEqual:@"Menu"]) [self performSegueWithIdentifier: @"goToSettings" sender: self];
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
    else if ([action isEqual:@"StartDefaultPlaylist"]) [self playAllSongs];
//    else if ([action isEqual:@"SongPicker"]) NSLog(@"Song picker");
    else if ([action isEqual:@"SongPicker"]) [self scrollingTimerKiller];
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
    [self setupHUD];
    NSLog(@"%f",[[defaults objectForKey:@"volumeSensitivity"] floatValue]);
}

- (void) decreaseVolume {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    float volume = mediaPlayer.volume;
    mediaPlayer.volume = volume-[[defaults objectForKey:@"volumeSensitivity"] floatValue];
    [self setupHUD];
}

- (void) playOrDefault {
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) [self playAllSongs];
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
    NSLog(@"scrollingTimerKiller");
    _marqueePosition=0;
    if ( [[self scrollingTimer] isValid]){
        [[self scrollingTimer] invalidate];
        _timersRunning--; NSLog(@"%d timers running",_timersRunning);
    }
}

-(void)marqueeTimerKiller {
    NSLog(@"marqueeTimerKiller");
    if ( [[self marqueeTimer] isValid]){
        [[self marqueeTimer] invalidate];
    }
}

// this always scrolls, even if the text fits.
-(void)scrollSongTitle:(id)parameter{
//    NSLog(@"%d timers running",_timersRunning);
    
    float textWidth = [_songTitle.text sizeWithFont:_songTitle.font].width;
    _marqueePosition=_marqueePosition+3;
    _adjustedSongFontSize = _songTitle.font.pointSize;
    _songTitle.frame=CGRectMake(20-(_marqueePosition), _songTitle.frame.origin.y, textWidth, _songTitle.frame.size.height);
    [_songTitle setFont:[UIFont systemFontOfSize:_adjustedSongFontSize]];
//    NSLog(@"scrolling font size %f",_adjustedSongFontSize);
    
    if (_marqueePosition>= textWidth+20) {
        [self scrollingTimerKiller];
        [self marqueeTimerKiller];
        _songTitle.frame=CGRectMake(20, _songTitle.frame.origin.y, textWidth, _songTitle.frame.size.height);
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
    // check the size. if the text doesn't fit, scroll it.
        self.marqueeTimer = [NSTimer scheduledTimerWithTimeInterval: 4
                                                      target: self
                                                    selector: @selector(startScrollingTimer)
                                                    userInfo: nil
                                                     repeats: NO];
    // otherwise the timer will restart itself to check for need due to new orientations
/*    else self.marqueeTimer = [NSTimer scheduledTimerWithTimeInterval: 4
                                                       target: self
                                                     selector: @selector(startTimer)
                                                     userInfo: nil
                                                      repeats: NO];
*/
}

- (void)startScrollingTimer {
    float textWidth = [_songTitle.text sizeWithFont:_songTitle.font].width;
    if ((_timersRunning==0) & (textWidth > self.view.bounds.size.width)) {
        [self scrollingTimerKiller];
        self.scrollingTimer = [NSTimer scheduledTimerWithTimeInterval:   0.05f
                                                    target: self
                                                  selector: @selector(scrollSongTitle:)
                                                  userInfo: nil
                                                   repeats: YES];
        _timersRunning++; NSLog(@"%d timers running", _timersRunning);
    }
}

/*
-(void)didRotateToInterfaceOrientation:(UIInterfaceOrientation)orientation duration:(NSTimeInterval)duration
{
    [self setupLabels];
    [self setupHUD];
}
 */

- (void)playAllSongs {
    //Create a query that will return all songs by The Beatles grouped by album
    MPMediaQuery* query = [MPMediaQuery songsQuery];
//    [query addFilterPredicate:[MPMediaPropertyPredicate predicateWithValue:@"The Beatles" forProperty:MPMediaItemPropertyArtist comparisonType:MPMediaPredicateComparisonEqualTo]];
//    [query setGroupingType:MPMediaGroupingAlbum];
    
    [mediaPlayer setQueueWithQuery: [MPMediaQuery songsQuery]];
//    plus = @"random";
    
    //Pass the query to the player
    [mediaPlayer setQueueWithQuery:query];
    //Start playing and set a label text to the name and image to the cover art of the song that is playing
    [mediaPlayer play];
}

@end