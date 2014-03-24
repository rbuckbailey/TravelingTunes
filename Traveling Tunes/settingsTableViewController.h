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
- (IBAction)artistAlignmentChanged:(id)sender;
- (IBAction)artistFontSizeSliderChanged:(id)sender;

@property (weak, nonatomic) IBOutlet UILabel *songFontSizeLabel;
@property (weak, nonatomic) IBOutlet UISlider *songFontSizeSlider;
@property (weak, nonatomic) IBOutlet UISegmentedControl *songAlignmentControl;
- (IBAction)songAlignmentChanged:(id)sender;
- (IBAction)songFontSizeSliderChanged:(id)sender;

@property (weak, nonatomic) IBOutlet UILabel *albumFontSizeLabel;
@property (weak, nonatomic) IBOutlet UISlider *albumFontSizeSlider;
@property (weak, nonatomic) IBOutlet UISegmentedControl *albumAlignmentControl;
- (IBAction)albumAlignmentControl:(id)sender;
- (IBAction)albumFontSizeSliderChanged:(id)sender;

//display theme cell outlets
@property (strong, nonatomic) IBOutlet UISwitch *themeInvert;
- (IBAction)themeInvertChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeGreyOnWhite;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeGreyOnBlack;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeLeaf;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeOlive;
@property (weak, nonatomic) IBOutlet UITableViewCell *themePeriwinkleBlue;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeLavender;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeBlush;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeHotDogStand;

// display preview cells and labels
@property (weak, nonatomic) IBOutlet UILabel *themeSelectionPreviewLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeSelectionPreview;
@property (weak, nonatomic) IBOutlet UILabel *customColorPreviewLabel2;
@property (weak, nonatomic) IBOutlet UITableViewCell *customColorPreview2;
@property (weak, nonatomic) IBOutlet UILabel *customColorPreviewLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *customColorPreview;

// custom color picker slider outlets
@property (weak, nonatomic) IBOutlet UISlider *textRedSlider;
@property (weak, nonatomic) IBOutlet UISlider *textGreenSlider;
@property (weak, nonatomic) IBOutlet UISlider *textBlueSlider;
@property (weak, nonatomic) IBOutlet UISlider *bgRedSlider;
@property (weak, nonatomic) IBOutlet UISlider *bgGreenSlider;
@property (weak, nonatomic) IBOutlet UISlider *bgBlueSlider;


// playlist setting outlets
@property (weak, nonatomic) IBOutlet UISwitch *playlistShuffle;
@property (weak, nonatomic) IBOutlet UISwitch *playlistRepeat;
- (IBAction)playlistShuffleChanged:(id)sender;
- (IBAction)playlistRepeatChanged:(id)sender;

// passes data from view to view
@property NSMutableDictionary *passthrough;

@end
