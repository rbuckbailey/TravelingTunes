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



MPMusicPlayerController*        mediaPlayer;
//may want to switch this to mediaplayer protocol which has built-in "current time" property
//MPMediaPlayback*        mediaPlayer;

@interface ttunesViewController ()
@end


@implementation ttunesViewController

- (IBAction)singleTapDetected:(id)sender {     NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
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
   
//    [self firstStartTimer];
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UIPanGestureRecognizer *recognizer = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleSwipe:)];
    [self.view addGestureRecognizer:recognizer];
}


/*** Gesture Actions begin ************************************************************************************************************************/


- (void)handleSwipe:(UIPanGestureRecognizer *)gesture {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    CGPoint translation = [gesture translationInView:self.view];
    NSString *key,*keyContinuous;
    swipeDirections oldDirection;

    oldDirection = _direction;
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
                       self.timer = [NSTimer scheduledTimerWithTimeInterval: 0.1f
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
                       self.timer = [NSTimer scheduledTimerWithTimeInterval: 0.1f
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
                       self.timer = [NSTimer scheduledTimerWithTimeInterval: 0.1f
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
                       self.timer = [NSTimer scheduledTimerWithTimeInterval: 0.1f
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
        NSLog(@"Stop");
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
    else if ([action isEqual:@"Next"]) { [mediaPlayer skipToNextItem]; }
    else if ([action isEqual:@"Previous"]) { [mediaPlayer skipToPreviousItem]; }
    else if ([action isEqual:@"RestartPrevious"]) { [self restartPrevious]; }
    else if ([action isEqual:@"Restart"]) { [mediaPlayer skipToBeginning]; }
    else if ([action isEqual:@"Rewind"]) NSLog(@"rewind");
    else if ([action isEqual:@"FastForward"]) NSLog(@"FF");
    else if ([action isEqual:@"VolumeUp"]) [self increaseVolume];
    else if ([action isEqual:@"VolumeDown"]) [self decreaseVolume];
    else if ([action isEqual:@"StartDefaultPlaylist"]) [self playAllSongs];
    else if ([action isEqual:@"SongPicker"]) NSLog(@"Song picker");
    [self setupLabels];
}

- (void) increaseVolume {
    float volume = mediaPlayer.volume;
    mediaPlayer.volume = volume+0.01f;
}

- (void) decreaseVolume {
    float volume = mediaPlayer.volume;
    mediaPlayer.volume = volume-0.01f;
}

- (void) playOrDefault {
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) [self playAllSongs];
    else [mediaPlayer play];
}

- (void) togglePlayPause {
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) [self playAllSongs];
    else if([mediaPlayer playbackState]==MPMusicPlaybackStatePlaying) {
        [mediaPlayer pause];
    } else {
        [mediaPlayer play];
    }
}

- (void) restartPrevious {
    if ([mediaPlayer currentPlaybackTime] < 2.5f) [mediaPlayer skipToPreviousItem]; else [mediaPlayer skipToBeginning];
}

// this always scrolls, even if the text fits.
-(void)scrollSongTitle:(id)parameter{
    static NSInteger len;
    NSString *scrollString = ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) ? @"Tap for default playlist." : [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle];
    int charactersLeft = [scrollString length]-len;
    if ((len+charactersLeft) > [scrollString length]) charactersLeft=[scrollString length]-len;
    _songTitle.text = [scrollString substringWithRange:NSMakeRange(len++,charactersLeft)];
    
    if (len==[scrollString length]) {
        [self.timer invalidate]; len=0; _songTitle.text = scrollString;// [self startTimer];
    }
}

// this super kludgey fix gives the labels a moment to set up before startTimer grabs the width
- (void)firstStartTimer {
    self.timer = [NSTimer scheduledTimerWithTimeInterval: 0.5f
                                                  target: self
                                                selector: @selector(startTimer)
                                                userInfo: nil
                                                 repeats: NO];
}

- (void)startTimer { // wait 4 seconds on title, then scroll it
    // check the size. if the text doesn't fit, scroll it.
    float textWidth = [_songTitle.text sizeWithFont:_songTitle.font].width;
    if (textWidth > _songTitle.frame.size.width)
        self.timer = [NSTimer scheduledTimerWithTimeInterval: 4
                                                      target: self
                                                    selector: @selector(scrollingTimer)
                                                    userInfo: nil
                                                     repeats: NO];
    // otherwise the timer will restart itself to check for need due to new orientations
    else self.timer = [NSTimer scheduledTimerWithTimeInterval: 4
                                                       target: self
                                                     selector: @selector(startTimer)
                                                     userInfo: nil
                                                      repeats: NO];

}

- (void)scrollingTimer {
    self.timer = [NSTimer scheduledTimerWithTimeInterval:   0.2f
                                                    target: self
                                                  selector: @selector(scrollSongTitle:)
                                                  userInfo: nil
                                                   repeats: YES];
}

-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)orientation duration:(NSTimeInterval)duration
{
    [self setupLabels];
}

- (void)setupLabels {
    int orientation = [[UIDevice currentDevice] orientation];
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
    int artistFontSize = (int)[[defaults objectForKey:@"artistFontSize"] floatValue];
    int songFontSize = (int)[[defaults objectForKey:@"songFontSize"] floatValue];
    int albumFontSize = (int)[[defaults objectForKey:@"albumFontSize"] floatValue];
    if (((orientation==1) | (orientation==2)) & [[defaults objectForKey:@"titleShrinkInPortrait"] isEqual:@"YES"]) {
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
        [_artistTitle sizeToFit];

    
        _songTitle.numberOfLines = 1;
        _songTitle.text   = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle];
        _songTitle.font   = [UIFont systemFontOfSize:songFontSize];
        _songTitle.textColor = themecolor;
        
        _albumTitle.numberOfLines = 1;
        _albumTitle.font    = [UIFont systemFontOfSize:albumFontSize];
        _albumTitle.text    = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyAlbumTitle];
        _albumTitle.textColor = themecolor;
        [_albumTitle setAlpha:0.6f];
        [_albumTitle sizeToFit];
    }
}

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