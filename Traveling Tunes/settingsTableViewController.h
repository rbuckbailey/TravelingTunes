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
@property (weak, nonatomic) IBOutlet UITableViewCell *startDefaultPlaylist;
@property (weak, nonatomic) IBOutlet settingsTableViewCell *ResetGestureAssignments;
@property (weak, nonatomic) IBOutlet UILabel *nothingCheck;
@property (weak, nonatomic) IBOutlet UILabel *playCheck;
@property (weak, nonatomic) IBOutlet UILabel *pauseCheck;
@property (weak, nonatomic) IBOutlet UILabel *playPauseCheck;
@property (weak, nonatomic) IBOutlet UILabel *volumeUpCheck;
@property (weak, nonatomic) IBOutlet UILabel *volumeDownCheck;
@property (weak, nonatomic) IBOutlet UILabel *fastForwardCheck;
@property (weak, nonatomic) IBOutlet UILabel *rewindCheck;
@property (weak, nonatomic) IBOutlet UILabel *nextCheck;
@property (weak, nonatomic) IBOutlet UILabel *restartCheck;
@property (weak, nonatomic) IBOutlet UILabel *previousCheck;
@property (weak, nonatomic) IBOutlet UILabel *restartPreviousCheck;
@property (weak, nonatomic) IBOutlet UILabel *songPickerCheck;
@property (weak, nonatomic) IBOutlet UILabel *startDefaultPlaylistCheck;
@property (weak, nonatomic) IBOutlet UILabel *menuCheck;

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
@property (weak, nonatomic) IBOutlet UILabel *themeGreyOnWhiteLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeGreyOnBlack;
@property (weak, nonatomic) IBOutlet UILabel *themeGreyOnBlackLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeLeaf;
@property (weak, nonatomic) IBOutlet UILabel *themeLeafLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeOlive;
@property (weak, nonatomic) IBOutlet UILabel *themeOliveLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themePeriwinkleBlue;
@property (weak, nonatomic) IBOutlet UILabel *themePeriwinkleBlueLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeLavender;
@property (weak, nonatomic) IBOutlet UILabel *themeLavenderLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeBlush;
@property (weak, nonatomic) IBOutlet UILabel *themeBlushLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeHotDogStand;
@property (weak, nonatomic) IBOutlet UILabel *themeHotDogStandLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeCustom;
@property (weak, nonatomic) IBOutlet UILabel *themeCustomLabel;
@property (weak, nonatomic) IBOutlet UILabel *themeGreyOnWhiteCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeGreyOnBlackCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeLavenderCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeBlushCheck;
@property (weak, nonatomic) IBOutlet UILabel *themePeriwinkleBlueCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeOliveCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeLeafCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeHotDogStandCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeCustomCheck;

// display preview cells and labels
@property (weak, nonatomic) IBOutlet UILabel *themeSelectionPreviewTitle;
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
