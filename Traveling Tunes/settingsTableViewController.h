//
//  settingsTableViewController.h
//  Traveling Tunes
//
//  Created by buck on 3/14/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "settingsTableViewCell.h"
#import "quickstartViewController.h"
#import <iAd/iAd.h>
#import <StoreKit/StoreKit.h>

#define kRemoveAdsProductIdentifier @"com.hgwt.ttfree.noAds"


// controls settings outlets
@interface settingsTableViewController : UITableViewController
<UIPageViewControllerDataSource>

- (void) closeInstructions;

#ifdef FREE
- (void)purchase;
- (void)restore;
- (void)tapsRemoveAdsButton;
- (IBAction)tapsRestoreButton:(id)sender;
#endif

@property (weak, nonatomic) IBOutlet UITableViewCell *adBannerCell;

@property (strong, nonatomic) UIPageViewController *pageController;

@property (weak, nonatomic) IBOutlet settingsTableViewCell *resetAllSettings;
@property (weak, nonatomic) IBOutlet settingsTableViewCell *instructions;

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
@property (weak, nonatomic) IBOutlet UITableViewCell *playCurrentArtist;
@property (weak, nonatomic) IBOutlet UITableViewCell *playCurrentAlbum;
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
@property (weak, nonatomic) IBOutlet UILabel *playCurrentAlbumCheck;
@property (weak, nonatomic) IBOutlet UILabel *playCurrentArtistCheck;

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

@property (weak, nonatomic) IBOutlet UISwitch *titleShrinkInPortrait;
@property (weak, nonatomic) IBOutlet UISwitch *titleShrinkLong;
@property (weak, nonatomic) IBOutlet UILabel *titleShrinkMinimumLabel;
@property (weak, nonatomic) IBOutlet UISlider *titleShrinkMinimumSlider;
- (IBAction)titleShrinkInPortraitChanged:(id)sender;
- (IBAction)titleShrinkToFitChanged:(id)sender;
- (IBAction)titleShrinkMinimumChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISwitch *titleScrollLong;
- (IBAction)titleScrollLongChanged:(id)sender;

@property (weak, nonatomic) IBOutlet UILabel *albumFontSizeLabel;
@property (weak, nonatomic) IBOutlet UISlider *albumFontSizeSlider;
@property (weak, nonatomic) IBOutlet UISegmentedControl *albumAlignmentControl;
- (IBAction)albumAlignmentControl:(id)sender;
- (IBAction)albumFontSizeSliderChanged:(id)sender;

//display theme cell outlets
@property (strong, nonatomic) IBOutlet UISwitch *themeInvert;
- (IBAction)themeInvertChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeWhiteOnGrey;
@property (weak, nonatomic) IBOutlet UILabel *themeWhiteOnGreyLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeGreyOnBlack;
@property (weak, nonatomic) IBOutlet UILabel *themeGreyOnBlackLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeLeaf;
@property (weak, nonatomic) IBOutlet UILabel *themeLeafLabel;
@property (weak, nonatomic) IBOutlet UITableViewCell *themeOldWest;
@property (weak, nonatomic) IBOutlet UILabel *themeOldWestLabel;
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
@property (weak, nonatomic) IBOutlet UILabel *themeWhiteOnGreyCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeGreyOnBlackCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeLavenderCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeBlushCheck;
@property (weak, nonatomic) IBOutlet UILabel *themePeriwinkleBlueCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeOldWestCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeLeafCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeHotDogStandCheck;
@property (weak, nonatomic) IBOutlet UILabel *themeCustomCheck;
@property (weak, nonatomic) IBOutlet UISwitch *showAlbumArt;
@property (weak, nonatomic) IBOutlet UISwitch *albumArtColors;
- (IBAction)albumArtColorsChanged:(id)sender;
- (IBAction)showAlbumArtChanged:(id)sender;



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

// HUD options
@property (weak, nonatomic) IBOutlet UISegmentedControl *HUDType;
@property (weak, nonatomic) IBOutlet UISegmentedControl *ScrubHUDType;
@property (weak, nonatomic) IBOutlet UISwitch *volumeAlwaysOn;
- (IBAction)volumeAlwaysOnChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISwitch *showStatusBar;
- (IBAction)showStatusBarChanged:(id)sender;
- (IBAction)HUDTypeChanged:(id)sender;
- (IBAction)scrubHUDTypeChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISwitch *dimAtNightSwitch;
- (IBAction)dimAtNightChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UILabel *sunSetLabel;
@property (weak, nonatomic) IBOutlet UILabel *sunRiseLabel;
@property (weak, nonatomic) IBOutlet UISlider *sunRiseSlider;
@property (weak, nonatomic) IBOutlet UISlider *sunSetSlider;
- (IBAction)sunRiseChanged:(id)sender;
- (IBAction)sunSetChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISwitch *invertAtNight;
- (IBAction)invertAtNightChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISwitch *showActions;
- (IBAction)showActionsChanged:(id)sender;




//continuous gesture sensitivity sliders
@property (weak, nonatomic) IBOutlet UISlider *volumeSensitivitySlider;
- (IBAction)volumeSensitivityChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISlider *seekSensitivitySlider;
- (IBAction)seekSensitivityChanged:(id)sender;



- (IBAction)rotationPortraitChanged:(id)sender;
- (IBAction)rotationClockwiseChanged:(id)sender;
- (IBAction)rotationAntiClockwiseChanged:(id)sender;
- (IBAction)rotationInvertedChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISwitch *rotationPortrait;
@property (weak, nonatomic) IBOutlet UISwitch *rotationClockwise;
@property (weak, nonatomic) IBOutlet UISwitch *rotationAntiClockwise;
@property (weak, nonatomic) IBOutlet UISwitch *rotationInverted;

// playlist setting outlets
@property (weak, nonatomic) IBOutlet UISwitch *playlistShuffle;
@property (weak, nonatomic) IBOutlet UISwitch *playlistRepeat;
- (IBAction)playlistShuffleChanged:(id)sender;
- (IBAction)playlistRepeatChanged:(id)sender;
- (IBAction)playOnLaunchChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISwitch *playOnLaunch;
@property (weak, nonatomic) IBOutlet UISwitch *pauseOnExit;
- (IBAction)pauseOnExitChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISlider *GPSSensitivitySlider;
@property (weak, nonatomic) IBOutlet UILabel *defaultPlaylistLabel;
- (IBAction)GPSSensitivityChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UISwitch *GPSVolumeToggle;
- (IBAction)GPSVolumeToggleChanged:(id)sender;
@property (weak, nonatomic) IBOutlet UILabel *GPSSensivityLabel;

@property (weak, nonatomic) IBOutlet UISwitch *disableAdBanners;
- (IBAction)disableAdBannersChanged:(id)sender;


// passes data from view to view
@property NSMutableDictionary *passthrough;

@end
