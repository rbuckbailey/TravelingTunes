//
//  playlistTableViewController.h
//  Traveling Tunes
//
//  Created by buck on 3/25/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <MediaPlayer/MediaPlayer.h>

@interface playlistTableViewController : UITableViewController
@property NSMutableDictionary *passthrough;
@property NSArray* playlists;

@end
