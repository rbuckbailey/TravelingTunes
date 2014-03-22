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
}

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
/*
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
 */

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


- (void)performPlayerAction:(NSString *)action :(NSString*)sender {
//    UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"mainStoryboard" bundle:nil];
//    settingsTableViewController *viewController = (settingsTableViewController *) [storyboard instantiateViewControllerWithIdentifier:@"settingsTable"];
    if ([action isEqual:@"Unassigned"]) _songTitle.text = [NSString stringWithFormat:@"%@ sent unassigned command",sender];
    else if ([action isEqual:@"Menu"]) [self performSegueWithIdentifier: @"goToSettings" sender: self];
    else if ([action isEqual:@"PlayPause"]) _songTitle.text = @"playPause";
    else if ([action isEqual:@"Play"]) _songTitle.text = @"play";
    else if ([action isEqual:@"Pause"]) _songTitle.text = @"Pause";
    else if ([action isEqual:@"NextSong"]) { _songTitle.text = @"skipToNext"; [mediaPlayer skipToNextItem]; }
    else if ([action isEqual:@"PreviousSong"]) { _songTitle.text = @"prevOrRestart"; [mediaPlayer skipToPreviousItem]; }
    else if ([action isEqual:@"RestartPrevious"]) { _songTitle.text = @"prevOrRestart"; [mediaPlayer skipToPreviousItem]; }
    else if ([action isEqual:@"Restart"]) { _songTitle.text = @"Restart"; [mediaPlayer skipToPreviousItem]; }
    else if ([action isEqual:@"Rewind"]) _songTitle.text = @"rewind";
    else if ([action isEqual:@"FastForward"]) _songTitle.text = @"FF";
    else if ([action isEqual:@"VolumeUp"]) _songTitle.text = @"vol up";
    else if ([action isEqual:@"VolumeDown"]) _songTitle.text = @"vol down";
    else if ([action isEqual:@"PlayAllBeatles"]) [self beatlesParty];
}

- (void)beatlesParty {
    _songTitle.text = @"BEATLES PARTY";         //Create a query that will return all songs by The Beatles grouped by album
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