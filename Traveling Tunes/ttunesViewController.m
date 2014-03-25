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

- (IBAction)singleTapDetected:(id)sender { }

- (id)init{
    return 0;
}

- (void)viewWillAppear:(BOOL)animated
{
    [self.navigationController setNavigationBarHidden:YES];
    [self setupLabels];
   
    [self firstStartTimer];
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

-(void)singleTap{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [self performPlayerAction:[defaults objectForKey:@"11Tap"]:@"11Tap"];
    NSLog(@"gesture is %@",[defaults objectForKey:@"1Tap"]);
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
//    gestureAssignmentController *assignments = [[gestureAssignmentController alloc] init];
//    _songTitle.text = assignments.panning;
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
    else if ([action isEqual:@"VolumeDown"]) _songTitle.text = @"vol down";
    else if ([action isEqual:@"PlayAllBeatles"]) [self beatlesParty];

 //   [self setupLabels];
    NSLog(@"%f",[mediaPlayer currentPlaybackTime]);
}

- (void) togglePlayPause {
    if([mediaPlayer playbackState]==MPMusicPlaybackStatePlaying) {
        [mediaPlayer pause];
    } else {
        [mediaPlayer play];
    }
}

- (void) restartPrevious {
    if ([mediaPlayer currentPlaybackTime] < 5) [mediaPlayer skipToPreviousItem]; else [mediaPlayer skipToBeginning];
}

/* backup of
- (void)fitLongTitle {
    NSString *text = @"This is a long sentence. Wonder how much space is needed?";
    CGFloat width = 100;
    CGFloat height = 100;
    bool sizeFound = false;
    while (!sizeFound) {
        NSLog(@"Begin loop");
        CGFloat fontSize = 14;
        CGFloat previousSize = 0.0;
        CGFloat currSize = 0.0;
        for (float fSize = fontSize; fSize < fontSize+6; fSize++) {
            CGRect r = [text boundingRectWithSize:CGSizeMake(width, height)
                                          options:NSStringDrawingUsesLineFragmentOrigin
                                       attributes:@{NSFontAttributeName:[UIFont systemFontOfSize:fSize]}
                                          context:nil];
            currSize =r.size.width*r.size.height;
            if (previousSize >= currSize) {
                width = width*11/10;
                height = height*11/10;
                fSize = fontSize+10;
            }
            else {
                previousSize = currSize;
            }
            NSLog(@"fontSize = %f\tbounds = (%f x %f) = %f",
                  fSize,
                  r.size.width,
                  r.size.height,r.size.width*r.size.height);
        }
        if (previousSize == currSize) {
            sizeFound = true;
        }
        
    }
    NSLog(@"Size found with width %f and height %f", width, height);
}
*/

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
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

 //   self.view.backgroundColor = [UIColor redColor];
    NSString *currentTheme = [[gestureController themes] objectForKey:@"current"];
    NSMutableDictionary *themedict = [gestureController themes];
    NSArray *themecolors = [themedict objectForKey:currentTheme];
    UIColor *themebg = [themecolors objectAtIndex:0];
    UIColor *themecolor = [themecolors objectAtIndex:1];
    
    
    self.view.backgroundColor = themebg;

    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) { //output "nothing playing screen" if nothing playing
        _artistTitle.numberOfLines = 1;
        _artistTitle.text   = @"No music playing.";
        _artistTitle.font   = [UIFont systemFontOfSize:28];
//        _artistTitle.textColor = [[[gestureController themes] objectForKey:[[gestureController themes] objectForKey:@"leaf"]] objectAtIndex:1];
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