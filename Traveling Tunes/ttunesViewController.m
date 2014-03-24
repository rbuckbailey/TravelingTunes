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
}

/*** Gesture Actions begin ************************************************************************************************************************/

- (IBAction)twoFingerTap:(id)sender {
    _songTitle.text=@"Two Fingers Detected";
}

- (IBAction)longPressDetected:(UIGestureRecognizer *)sender {
    if (sender.state == UIGestureRecognizerStateBegan) {
        gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
        NSMutableDictionary *assignments = [gestureController assignments];
        [self performPlayerAction:[assignments objectForKey:@"1LongPress"]:@"1LongPress"];
    } // else, UIGestureRecognizerState[Changed / Ended]
}

-(void)singleTap{
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    [self performPlayerAction:[gestureController.assignments objectForKey:@"11Tap"]:@"11Tap"];
}
-(void)doubleTap{
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    [self performPlayerAction:[gestureController.assignments objectForKey:@"12Tap"]:@"12Tap"];
}
-(void)tripleTap{
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    [self performPlayerAction:[gestureController.assignments objectForKey:@"13Tap"]:@"13Tap"];
}
-(void)quadrupleTap{
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    [self performPlayerAction:[gestureController.assignments objectForKey:@"14Tap"]:@"14Tap"];
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
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSMutableDictionary *assignments = [gestureController assignments];
    [self performPlayerAction:[assignments objectForKey:@"1SwipeLeft"]:@"swipeLeft"];
}

- (IBAction)swipeRight:(id)sender {
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSMutableDictionary *assignments = [gestureController assignments];
    [self performPlayerAction:[assignments objectForKey:@"1SwipRight"]:@"swipeRight"];
}

- (IBAction)swipeUpDetected:(id)sender {
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSMutableDictionary *assignments = [gestureController assignments];
    [self performPlayerAction:[assignments objectForKey:@"1SwipeUp"]:@"swipeUp"];
}

- (IBAction)swipeDownDetected:(id)sender {
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSMutableDictionary *assignments = [gestureController assignments];
    [self performPlayerAction:[assignments objectForKey:@"1SwipeDown"]:@"swipeDown"];
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

    [self setupLabels];
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

/* from StackOverflow, marquee code from OSX. 
 ********************************************
-(void)awakeFromNib{
    self.myTimer=[NSTimer new];
    self.fullString=@"This is a long string to be shown.";
    [self.label setAlignment:NSRightTextAlignment];
}
-(void)scrollText:(id)parameter{
    static NSInteger len;
//    [self.label setStringValue:[self.fullString substringWithRange:NSMakeRange(0, len++)]];
    _songTitle.text = @"Artist Test This is a long string where does it cut off";

    
    if (self.label.stringValue.length==self.fullString.length) {
        [self.myTimer invalidate];
    }
}
- (IBAction)buttonAction:(id)sender {
    self.myTimer = [NSTimer scheduledTimerWithTimeInterval:0.2f
                                                    target: self
                                                  selector:@selector(scrollText:)
                                                  userInfo: nil
                                                   repeats:YES];
}
************************************************/

- (void)setupLabels {
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
   
    if ([mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]==NULL) { //output "none playing screen"
        _artistTitle.numberOfLines = 1;
        _artistTitle.text   = @"No music playing.";
        _artistTitle.font   = [UIFont systemFontOfSize:30];
        
        _songTitle.numberOfLines = 1;
        _songTitle.text   = @"Tap screen to play default playlist.";
        _songTitle.font   = [UIFont systemFontOfSize:30];
        
        _albumTitle.numberOfLines = 1;
        _albumTitle.text    = @"...";
        _albumTitle.font    =  [UIFont systemFontOfSize:30];
    } else { // output song titles and info
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