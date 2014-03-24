//
//  gestureAssignmentController.h
//  Traveling Tunes
//
//  Created by buck on 3/10/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#ifndef Traveling_Tunes_gestureAssignmentController_h
#define Traveling_Tunes_gestureAssignmentController_h

@interface gestureAssignmentController : NSObject

@property (retain) NSMutableDictionary* themes;

- (void)initGestureAssignments;
- (void)initDisplaySettings;
- (void)initPlaylistSettings;
- (void)initThemes;
- (void)saveThemes;

@end


#endif
