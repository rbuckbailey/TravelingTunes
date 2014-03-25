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
   
    [self firstStartTimer];
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UIPanGestureRecognizer *recognizer = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handleSwipe:)];
    [self.view addGestureRecognizer:recognizer];
}

- (void)handleSwipe:(UIPanGestureRecognizer *)gesture
{
    CGPoint translation = [gesture translationInView:self.view];
    
    if (gesture.state == UIGestureRecognizerStateBegan)
    {
        _direction = directionNone;
    }
    else if (gesture.state == UIGestureRecognizerStateChanged && _direction == directionNone)
    {
        _direction = [self determineSwipeDirectiond:translation];
        
        // ok, now initiate movement in the direction indicated by the user's gesture
        
       switch (_direction) {
            case directionDown:
                NSLog(@"Start moving down");
                break;
                
            case directionUp:
                NSLog(@"Start moving up");
                break;
                
            case directionRight:
                NSLog(@"Start moving right");
                break;
                
            case directionLeft:
                NSLog(@"Start moving left");
                break;
                
            default:
                break;
        }
    }
    else if (gesture.state == UIGestureRecognizerStateEnded)
    {
        // now tell the camera to stop
        NSLog(@"Stop");
    }
}

- (swipeDirections)determineSwipeDirectiond:(CGPoint)translation
{
    if (_direction != directionNone)
        return _direction;
    
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


/*** Gesture Actions begin ************************************************************************************************************************/

- (IBAction)twoFingerTap:(id)sender {
    _songTitle.text=@"Two Fingers Detected";
}

- (IBAction)longPressDetected:(UIGestureRecognizer *)sender {
    if (sender.state == UIGestureRecognizerStateBegan) {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        [self performPlayerAction:[defaults objectForKey:@"1LongPress"]:@"1LongPress"];
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
- (IBAction)pinchDetected:(id)sender {
    CGFloat scale = [(UIPinchGestureRecognizer *)sender scale];
    CGFloat velocity = [(UIPinchGestureRecognizer *)sender velocity];
    NSString *resultString = [[NSString alloc] initWithFormat:
                              @"Pinch - scale = %f, velocity = %f",
                              scale, velocity];
    _songTitle.text = resultString;
}

- (IBAction)rotationDetected:(id)sender {
    CGFloat radians = [(UIRotationGestureRecognizer *)sender rotation];
    CGFloat velocity = [(UIRotationGestureRecognizer *)sender velocity];
    NSString *resultString = [[NSString alloc] initWithFormat:
                              @"Rotation - Radians = %f, velocity = %f",
                              radians, velocity];
    _songTitle.text = resultString;
}
 

- (IBAction)panDetected:(id)sender {
/*    gestureAssignmentController *assignments = [[gestureAssignmentController alloc] init];
    if(sender.state == UIGestureRecognizerStateBegan) _panning = NO;
    
    CGPoint v =[sender velocity];
    
    NSLog(@"%f, %f",v.x,v.y);
    
    if( (abs(v.x) >= UMBRAL) && !_panning)
    {
        _panning = YES;
        [sender cancelsTouchesInView];
        
        if(v.x>0) NSLog(@"Right");
        else NSLog(@"Left");
        
        [self doSomething];
    }
 */
}

- (IBAction)swipeLeftDetected:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"1SwipeLeft"]:@"swipeLeft"];
}

- (IBAction)swipeRight:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"1SwipRight"]:@"swipeRight"];
}

- (IBAction)swipeUpDetected:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"1SwipeUp"]:@"swipeUp"];
}

- (IBAction)swipeDownDetected:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"1SwipeDown"]:@"swipeDown"];
}

/****** Gesture Actions end *********************************************************************************************************************************/
/****** Player Actions begin *********************************************************************************************************************************/

- (void)performPlayerAction:(NSString *)action :(NSString*)sender {
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    if ([action isEqual:@"Unassigned"]) NSLog(@"%@ sent unassigned command",sender);
    else if ([action isEqual:@"Menu"]) [self performSegueWithIdentifier: @"goToSettings" sender: self];
    else if ([action isEqual:@"PlayPause"]) [self togglePlayPause];
    else if ([action isEqual:@"Play"]) [mediaPlayer play];
    else if ([action isEqual:@"Pause"]) [mediaPlayer pause];
    else if ([action isEqual:@"NextSong"]) { [mediaPlayer skipToNextItem]; }
    else if ([action isEqual:@"PreviousSong"]) { [mediaPlayer skipToPreviousItem]; }
    else if ([action isEqual:@"RestartPrevious"]) { [self restartPrevious]; }
    else if ([action isEqual:@"Restart"]) { [mediaPlayer skipToBeginning]; }
    else if ([action isEqual:@"Rewind"]) _songTitle.text = @"rewind";
    else if ([action isEqual:@"FastForward"]) _songTitle.text = @"FF";
    else if ([action isEqual:@"VolumeUp"]) _songTitle.text = @"vol up";
//    else if ([action isEqual:@"VolumeDown"]) _songTitle.text = @"vol down";
    else if ([action isEqual:@"VolumeDown"]) [self beatlesParty];
    else if ([action isEqual:@"PlayAllBeatles"]) [self beatlesParty];

    NSLog(@"%f",[mediaPlayer currentPlaybackTime]);
}

- (void) togglePlayPause {
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    if([mediaPlayer playbackState]==MPMusicPlaybackStatePlaying) {
        [mediaPlayer pause];
    } else {
        [mediaPlayer play];
    }
}

- (void) restartPrevious {
    if ([mediaPlayer currentPlaybackTime] < 5) [mediaPlayer skipToPreviousItem]; else [mediaPlayer skipToBeginning];
}

// this always scrolls, even if the text fits.
-(void)scrollSongTitle:(id)parameter{
    static NSInteger len;
    NSString *scrollString = ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) ? @"Tap for default playlist." : [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle];
    int charactersLeft = [scrollString length]-len;
    if ((len+charactersLeft) > [scrollString length]) charactersLeft=[scrollString length]-len;
    _songTitle.text = [scrollString substringWithRange:NSMakeRange(len++,charactersLeft)];
    
    if (len==[scrollString length]) {
        [self.timer invalidate]; len=0; _songTitle.text = scrollString; [self startTimer];
    }
}

// this super kludgey fix gives the labels a moment to set up before startTimer grabs the width
- (void)firstStartTimer {
    self.timer = [NSTimer scheduledTimerWithTimeInterval: 0.1f
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

//- (IBAction)buttonAction:(id)sender {
- (void)scrollingTimer {
    self.timer = [NSTimer scheduledTimerWithTimeInterval:   0.2f
                                                    target: self
                                                  selector: @selector(scrollSongTitle:)
                                                  userInfo: nil
                                                   repeats: YES];
}

- (void)setupLabels {
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    NSLog(@"Current theme should be %@",[defaults objectForKey:@"currentTheme"]);
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
    NSLog(@"invert is %@",[defaults objectForKey:@"themeInvert"]);
    
    self.view.backgroundColor = themebg;

    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) { //output "nothing playing screen" if nothing playing
        _artistTitle.numberOfLines = 1;
        _artistTitle.text   = @"No music playing.";
        _artistTitle.font   = [UIFont systemFontOfSize:28];
        _artistTitle.textColor = themecolor;
        [_artistTitle setAlpha:0.6f];
        
        _songTitle.numberOfLines = 1;
        _songTitle.text   = @"Tap for default playlist.";
        _songTitle.font   = [UIFont systemFontOfSize:28];
        _songTitle.textColor = themecolor;
        
        _albumTitle.numberOfLines = 1;
        _albumTitle.text    = @"Long hold for menu.";
        _albumTitle.font    =  [UIFont systemFontOfSize:28];
        _albumTitle.textColor = themecolor;
        [_albumTitle setAlpha:0.6f];
    } else {
        _artistTitle.numberOfLines = 1;
        _artistTitle.text   = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyArtist];
        _artistTitle.font   = [UIFont systemFontOfSize:(int)[[[gestureController displaySettings] objectForKey:@"artistFontSize"] floatValue]];
    
        _songTitle.numberOfLines = 1;
        _songTitle.text   = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle];
        _songTitle.font   = [UIFont systemFontOfSize:(int)[[[gestureController displaySettings] objectForKey:@"songFontSize"] floatValue]];
        
        _albumTitle.numberOfLines = 1;
        _albumTitle.font    = [UIFont systemFontOfSize:(int)[[[gestureController displaySettings] objectForKey:@"albumFontSize"] floatValue]];
        _albumTitle.text    = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyAlbumTitle];

    }
}

- (void)beatlesParty {
    //Create a query that will return all songs by The Beatles grouped by album
    MPMediaQuery* query = [MPMediaQuery songsQuery];
    [query addFilterPredicate:[MPMediaPropertyPredicate predicateWithValue:@"The Beatles" forProperty:MPMediaItemPropertyArtist comparisonType:MPMediaPredicateComparisonEqualTo]];
    [query setGroupingType:MPMediaGroupingAlbum];
    //Pass the query to the player
    [mediaPlayer setQueueWithQuery:query];
    //Start playing and set a label text to the name and image to the cover art of the song that is playing
    [mediaPlayer play];
    _songTitle.text = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle];
}


@end