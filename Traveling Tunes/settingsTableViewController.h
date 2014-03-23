//
//  settingsTableViewController.h
//  Traveling Tunes
//
//  Created by buck on 3/14/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "settingsTableViewCell.h"

// controls settings outlets
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
@property (weak, nonatomic) IBOutlet UITableViewCell *VolumeUp;
@property (weak, nonatomic) IBOutlet UITableViewCell *VolumeDown;
@property (weak, nonatomic) IBOutlet settingsTableViewCell *ResetGestureAssignments;

// display settings outlets
@property (weak, nonatomic) IBOutlet UILabel *artistFontSizeLabel;
@property (weak, nonatomic) IBOutlet UISlider *artistFontSizeSlider;
@property (weak, nonatomic) IBOutlet UISegmentedControl *artistAlignmentControl;
@property (weak, nonatomic) IBOutlet UILabel *artistColorLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *artistColorCell;
- (IBAction)artistAlignmentChanged:(id)sender;
- (IBAction)artistFontSizeSliderChanged:(id)sender;

@property (weak, nonatomic) IBOutlet UISlider *songFontSize;
@property (weak, nonatomic) IBOutlet UISlider *songFontSizeSlider;

@property (weak, nonatomic) IBOutlet UISlider *albumFontSize;
@property (weak, nonatomic) IBOutlet UISlider *albumFontSizeSlider;

// custom color picker slider outlets
@property (weak, nonatomic) IBOutlet UISlider *textRedSlider;
@property (weak, nonatomic) IBOutlet UISlider *textGreenSlider;
@property (weak, nonatomic) IBOutlet UISlider *textBlueSlider;
@property (weak, nonatomic) IBOutlet UISlider *bgRedSlider;
@property (weak, nonatomic) IBOutlet UISlider *bgGreenSlider;
@property (weak, nonatomic) IBOutlet UISlider *bgBlueSlider;
// custom color picker example outlets
@property (weak, nonatomic) IBOutlet UILabel *customExampleLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *customExampleCell;

// passes data from view to view
@property NSMutableDictionary *passthrough;

@end
