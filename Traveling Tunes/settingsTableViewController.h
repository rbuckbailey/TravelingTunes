//
//  settingsTableViewController.h
//  Traveling Tunes
//
//  Created by buck on 3/14/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "settingsTableViewCell.h"

@interface settingsTableViewController : UITableViewController
@property (weak, nonatomic) IBOutlet UITableViewCell *Nothing;
@property (weak, nonatomic) IBOutlet UITableViewCell *Play;
@property (weak, nonatomic) IBOutlet UITableViewCell *Pause;
@property (weak, nonatomic) IBOutlet UITableViewCell *PlayPause;
@property (weak, nonatomic) IBOutlet UITableViewCell *FastForward;
@property (weak, nonatomic) IBOutlet UITableViewCell *Next;
@property (weak, nonatomic) IBOutlet UITableViewCell *Rewind;
@property (weak, nonatomic) IBOutlet UITableViewCell *Previous;
@property (weak, nonatomic) IBOutlet UITableViewCell *Restart;
@property (weak, nonatomic) IBOutlet UITableViewCell *RestartPrevious;
@property (weak, nonatomic) IBOutlet UITableViewCell *SongPicker;
@property (weak, nonatomic) IBOutlet UITableViewCell *Menu;

@property NSMutableDictionary *passthrough;
@end
