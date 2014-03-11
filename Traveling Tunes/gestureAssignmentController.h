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
#define PLAYALLBEATLES  12
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
@property (atomic) int singleTap2;
@property (atomic) int doubleTap2;
@property (atomic) int longPress2;
@property (atomic) int tripleTap2;
@property (atomic) int swipeLeft2;
@property (atomic) int swipeRight2;
@property (atomic) int swipeUp2;
@property (atomic) int swipeDown2;
@property (atomic) int singleTap3;
@property (atomic) int doubleTap3;
@property (atomic) int longPress3;
@property (atomic) int tripleTap3;
@property (atomic) int swipeLeft3;
@property (atomic) int swipeRight3;
@property (atomic) int swipeUp3;
@property (atomic) int swipeDown3;

@end


#endif
