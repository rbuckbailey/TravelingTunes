//
//  gestureAssignmentController.h
//  Traveling Tunes
//
//  Created by buck on 3/10/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#ifndef Traveling_Tunes_gestureAssignmentController_h
#define Traveling_Tunes_gestureAssignmentController_h

#define UNASSIGNED      0
#define MENU            1
#define PLAY            2
#define PAUSE           3
#define PLAYPAUSE       4
#define NEXTSONG        5
#define PREVIOUSSONG    6
#define PREVIOUSRESTART 7
#define REWIND          8
#define FASTFORWARD     9
#define VOLUMEUP        10
#define VOLUMEDOWN      11
#define SONGPICKER      12
#define PLAYALLBEATLES  42
/*
 consider:  PLAYALLSHUFFLE
            PLAYSHUFFLEDALBUMS
            PLAYARTISTSHUFFLED ... ARTISTSHUFFLEDBYALBUM
            RESTARTSONG (no skip back)
            GENIUSPLAYLIST
            RATESONG ?
            SHARE/TWEET/FB SONG ?
            SKIP FORWARD/BACK 5, 10, 20 SECONDS
 */

@interface gestureAssignmentController : NSObject

@property (atomic) int singleTap;
@property (atomic) int doubleTap;
@property (atomic) int longPress;
@property (atomic) int tripleTap;
@property (atomic) int swipeLeft;
@property (atomic) int swipeRight;
@property (atomic) int swipeUp;
@property (atomic) int swipeDown;

@end


#endif
