//
//  ttunesViewController.m
//  Traveling Tunes
//
//  Created by buck on 1/31/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "ttunesViewController.h"

MPMusicPlayerController*        mediaPlayer;
gestureAssignmentController *assignments;



@interface ttunesViewController ()
@end



@implementation ttunesViewController

- (id)init {
//    assignments = [gestureAssignmentController init];
    mediaPlayer = [MPMusicPlayerController iPodMusicPlayer];
    return 0;
}

- (IBAction)tripleTapDetected:(id)sender {
    
    //init music player object

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


- (IBAction)doubleTapDetected:(id)sender {
    _songTitle.text=@"narf!";
}

- (IBAction)singleTapDetected:(id)sender {
    //assignments.singleTap = @"foo";
    [assignments setSingleTap:@"foo"];
    _songTitle.text = [assignments singleTap];
}

- (IBAction)longPressDetected:(id)sender {
    _songTitle.text=@"Long Press";
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
//    _songTitle.text=@"Panning";
}

- (IBAction)swipeLeftDetected:(id)sender {
    _songTitle.text=@"Previous Song";
    [mediaPlayer skipToPreviousItem];
}

- (IBAction)swipeRight:(id)sender {
    _songTitle.text=@"Next Song";
   [mediaPlayer skipToNextItem];
}

- (IBAction)swipeUpDetected:(id)sender {
    _songTitle.text=@"Swipe Up";
}

- (IBAction)swipeDownDetected:(id)sender {
    _songTitle.text=@"Swipe Down";
}

@end