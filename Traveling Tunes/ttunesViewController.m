//
//  ttunesViewController.m
//  Traveling Tunes
//
//  Created by buck on 1/31/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "ttunesViewController.h"
#import "gestureAssignmentController.h"


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

- (IBAction)longPressDetected:(id)sender {
    gestureAssignmentController *assignments = [[gestureAssignmentController alloc] init];
    int temp = [assignments longPress];
    [self selectActionFromString:temp:@"longPress"];
}

-(void)singleTap{
    _songTitle.text = @"Single Tap Detected";
}
-(void)doubleTap{
    _songTitle.text=@"Double Tap Detected";
}
-(void)tripleTap{
    _songTitle.text=@"Triple Tap Detected";
}
-(void)quadrupTap{
    _songTitle.text=@"Quadrup Tap Detected";
}

- (IBAction)twoFingerTap:(id)sender {
    _songTitle.text=@"Two Fingers Detected";
}

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    UITouch *touch = [touches anyObject];
    NSUInteger tapCount = [touch tapCount];
    switch (tapCount) {
        case 1:
            [self singleTap];
            break;
        case 2:
            [self doubleTap];
            break;
        case 3:
            [self tripleTap];
            break;
        case 4:
            [self quadrupTap];
            break;
        default :
            break;
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
    gestureAssignmentController *assignments = [[gestureAssignmentController alloc] init];
    int temp = [assignments swipeLeft];
    [self selectActionFromString:temp:@"swipeLeft"];
}

- (IBAction)swipeRight:(id)sender {
    gestureAssignmentController *assignments = [[gestureAssignmentController alloc] init];
    int temp = [assignments swipeRight];
    [self selectActionFromString:temp:@"swipeRight"];
}

- (IBAction)swipeUpDetected:(id)sender {
    gestureAssignmentController *assignments = [[gestureAssignmentController alloc] init];
    int temp = [assignments swipeUp];
    [self selectActionFromString:temp:@"swipeUp"];
}

- (IBAction)swipeDownDetected:(id)sender {
    gestureAssignmentController *assignments = [[gestureAssignmentController alloc] init];
    int temp = [assignments swipeDown];
    [self selectActionFromString:temp:@"swipeDown"];
}


- (void)selectActionFromString:(int)action :(NSString*)sender {
    switch (action) {
        case UNASSIGNED: _songTitle.text = [NSString stringWithFormat:@"%@ sent unassigned command",sender]; break;
        case PLAY: _songTitle.text = @"play"; break;
        case PAUSE: _songTitle.text = @"Pause"; break;
        case PLAYPAUSE: _songTitle.text = @"playPause"; break;
        case MENU: [self performSegueWithIdentifier: @"goToSettings" sender: self]; break;
        case NEXTSONG:  _songTitle.text = @"skipToNext"; [mediaPlayer skipToNextItem]; break;
        case PREVIOUSSONG:  _songTitle.text = @"skipToPrevious"; [mediaPlayer skipToPreviousItem]; break;
            //if (currentSong.currentPlaybackTime >= 5) { /* restart the song  } else { }
        case PREVIOUSRESTART:  _songTitle.text = @"prevOrRestart"; [mediaPlayer skipToPreviousItem]; break;
        case REWIND:  _songTitle.text = @"rewind"; break;
        case FASTFORWARD:  _songTitle.text = @"FF"; break;
        case VOLUMEUP:  _songTitle.text = @"vol up"; break;
        case VOLUMEDOWN:  _songTitle.text = @"vol down"; break;
        case PLAYALLBEATLES:  _songTitle.text = @"BEATLES PARTY";         //Create a query that will return all songs by The Beatles grouped by album
            MPMediaQuery* query = [MPMediaQuery songsQuery];
            [query addFilterPredicate:[MPMediaPropertyPredicate predicateWithValue:@"The Beatles" forProperty:MPMediaItemPropertyArtist comparisonType:MPMediaPredicateComparisonEqualTo]];
            [query setGroupingType:MPMediaGroupingAlbum];
            
            //Pass the query to the player
            [mediaPlayer setQueueWithQuery:query];
            
            //Start playing and set a label text to the name and image to the cover art of the song that is playing
            [mediaPlayer play];
            _songTitle.text = [mediaPlayer.nowPlayingItem valueForProperty:MPMediaItemPropertyTitle]; break;
    }
}


@end