//
//  settingsTableViewController.m
//  Traveling Tunes
//
//  Created by buck on 3/14/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "settingsTableViewController.h"
#import "settingsTableViewCell.h"
#import "gestureAssignmentController.h"


@interface settingsTableViewController ()
@end

@implementation settingsTableViewController

-(void)viewWillAppear:(BOOL)animated
{
    [self.navigationController setNavigationBarHidden:NO];
    
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
//    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];

    //initialize check marks Action Selector view
    _themeSelectionPreviewLabel.text = [defaults objectForKey:@"currentTheme"];
    // only set the check marks for actions if we're all the way inside that thread
    [self setUpActionChecks];
    [self setUpThemeChecks];
    
    // initialize slider controls
    _artistFontSizeLabel.text = [NSString stringWithFormat:@"%@",[defaults objectForKey:@"artistFontSize"]];
    _artistFontSizeSlider.value = (int)[[defaults objectForKey:@"artistFontSize"] floatValue];
    _artistAlignmentControl.selectedSegmentIndex = (int)[[defaults objectForKey:@"artistAlignment"] floatValue];
    _songFontSizeLabel.text = [NSString stringWithFormat:@"%@",[defaults objectForKey:@"songFontSize"]];
    _songFontSizeSlider.value = (int)[[defaults objectForKey:@"songFontSize"] floatValue];
    _songAlignmentControl.selectedSegmentIndex = (int)[[defaults objectForKey:@"songAlignment"] floatValue];
    _albumFontSizeLabel.text = [NSString stringWithFormat:@"%@",[defaults objectForKey:@"albumFontSize"]];
    _albumFontSizeSlider.value = (int)[[defaults objectForKey:@"albumFontSize"] floatValue];
    _albumAlignmentControl.selectedSegmentIndex = (int)[[defaults objectForKey:@"albumAlignment"] floatValue];

    _titleShrinkMinimumSlider.value = (int)[[defaults objectForKey:@"minimumFontSize"] floatValue];
    _titleShrinkMinimumLabel.text = [NSString stringWithFormat:@"%@",[defaults objectForKey:@"minimumFontSize"]];
    _sunRiseSlider.value = (int)[[defaults objectForKey:@"SunRiseHour"] floatValue];
    _sunSetSlider.value = (int)[[defaults objectForKey:@"SunSetHour"] floatValue];
    NSString *labelText;
    if ((int)[[defaults objectForKey:@"SunSetHour"] floatValue] > 12) {
        labelText = [[NSString stringWithFormat:@"%i",(int)([[defaults objectForKey:@"SunSetHour"] floatValue]-12)] stringByAppendingString:@" pm"];
    } else if ((int)[[defaults objectForKey:@"SunSetHour"] floatValue] == 12)
        labelText = [[NSString stringWithFormat:@"%i",(int)([[defaults objectForKey:@"SunSetHour"] floatValue])] stringByAppendingString:@" pm"];
    else labelText = [[NSString stringWithFormat:@"%i",(int)[[defaults objectForKey:@"SunSetHour"] floatValue]] stringByAppendingString:@" am"];
    _sunSetLabel.text = labelText;
    if ((int)[[defaults objectForKey:@"SunRiseHour"] floatValue] > 12) {
        labelText = [[NSString stringWithFormat:@"%i",(int)([[defaults objectForKey:@"SunRiseHour"] floatValue]-12)] stringByAppendingString:@" pm"];
    } else if ((int)[[defaults objectForKey:@"SunRiseHour"] floatValue] == 12)
        labelText = [[NSString stringWithFormat:@"%i",(int)([[defaults objectForKey:@"SunRiseHour"] floatValue])] stringByAppendingString:@" pm"];
    else labelText = [[NSString stringWithFormat:@"%i",(int)[[defaults objectForKey:@"SunRiseHour"] floatValue]] stringByAppendingString:@" am"];
    _sunRiseLabel.text = labelText;

    
    _bgRedSlider.value = (int)[[defaults objectForKey:@"customBGRed"] floatValue];
    _bgGreenSlider.value = (int)[[defaults objectForKey:@"customBGGreen"] floatValue];
    _bgBlueSlider.value = (int)[[defaults objectForKey:@"customBGBlue"] floatValue];
    _textRedSlider.value = (int)[[defaults objectForKey:@"customTextRed"] floatValue];
    _textGreenSlider.value = (int)[[defaults objectForKey:@"customTextGreen"] floatValue];
    _textBlueSlider.value = (int)[[defaults objectForKey:@"customTextBlue"] floatValue];
    _volumeSensitivitySlider.value = [[defaults objectForKey:@"volumeSensitivity"] floatValue];
    _seekSensitivitySlider.value = [[defaults objectForKey:@"seekSensitivity"] floatValue];
    
    // initialized segmented switches
    _HUDType.selectedSegmentIndex = (int)[[defaults objectForKey:@"HUDType"] floatValue];
    _ScrubHUDType.selectedSegmentIndex = (int)[[defaults objectForKey:@"ScrubHUDType"] floatValue];


    // initialize theme previews for Display settings
    [self setThemeLabels];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) {
        _themeInvert.on = YES;
/*        _themeCustomLabel.textColor = themecolor;
        _themeCustom.backgroundColor = themebg;
        [self invertThemeLabels]; */
    }
    else {
        _themeInvert.on = NO; }

    NSLog(@"Custom color is fore r %f g %f b %f back %f g %f b %f",_textRedSlider.value,_textGreenSlider.value,_textBlueSlider.value,_bgRedSlider.value,_bgGreenSlider.value,_bgBlueSlider.value);
    [self updateCustomPreviews];
    
    // initialize more switches
    if ([[defaults objectForKey:@"shuffle"] isEqual:@"YES"]) _playlistShuffle.on = YES; else _playlistShuffle.on = NO;
    if ([[defaults objectForKey:@"repeat"] isEqual:@"YES"]) _playlistRepeat.on = YES; else _playlistRepeat.on = NO;
    if ([[defaults objectForKey:@"VolumeAlwaysOn"] isEqual:@"YES"]) _volumeAlwaysOn.on = YES; else _volumeAlwaysOn.on = NO;
    if ([[defaults objectForKey:@"ShowStatusBar"] isEqual:@"YES"]) _showStatusBar.on = YES; else _showStatusBar.on = NO;
    if ([[defaults objectForKey:@"RotationClockwise"] isEqual:@"YES"]) _rotationClockwise.on = YES; else _rotationClockwise.on = NO;
    if ([[defaults objectForKey:@"RotationAntiClockwise"] isEqual:@"YES"]) _rotationAntiClockwise.on = YES; else _rotationAntiClockwise.on = NO;
    if ([[defaults objectForKey:@"RotationInverted"] isEqual:@"YES"]) _rotationInverted.on = YES; else _rotationInverted.on = NO;
    if ([[defaults objectForKey:@"RotationPortrait"] isEqual:@"YES"]) _rotationPortrait.on = YES; else _rotationPortrait.on = NO;
    if ([[defaults objectForKey:@"PlayOnLaunch"] isEqual:@"YES"]) _playOnLaunch.on = YES; else _playOnLaunch.on = NO;
    if ([[defaults objectForKey:@"PauseOnExit"] isEqual:@"YES"]) _pauseOnExit.on = YES; else _pauseOnExit.on = NO;
    if ([[defaults objectForKey:@"InvertAtNight"] isEqual:@"YES"]) _invertAtNight.on = YES; else _invertAtNight.on = NO;
    if ([[defaults objectForKey:@"DimAtNight"] isEqual:@"YES"]) _dimAtNightSwitch.on = YES; else _dimAtNightSwitch.on = NO;
    if ([[defaults objectForKey:@"titleShrinkLong"] isEqual:@"YES"]) _titleShrinkLong.on = YES; else _titleShrinkLong.on = NO;
    if ([[defaults objectForKey:@"titleShrinkInPortrait"] isEqual:@"YES"]) _titleShrinkInPortrait.on = YES; else _titleShrinkInPortrait.on = NO;

    // to insert Navigation View titles
    //self.navigationItem.title = @"Test";
}

- (void) popToRoot {
    [self.navigationController popToRootViewControllerAnimated:YES]; //requires iOS 7+
}

- (void)viewDidLoad {
    [super viewDidLoad];
    UIBarButtonItem *rightButton = [[UIBarButtonItem alloc] initWithTitle:@"Done"
                                                                    style:UIBarButtonItemStyleDone target:self action:@selector(popToRoot)];
    self.navigationItem.rightBarButtonItem = rightButton;
}


- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)controlButtonTap:(id)sender {

    [self performSegueWithIdentifier: @"nextSettings" sender: self];

}

/*
// Override to support conditional editing of the table view.
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    // Return NO if you do not want the specified item to be editable.
    return YES;
}
*/

/*
// Override to support editing the table view.
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        // Delete the row from the data source
        [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
    } else if (editingStyle == UITableViewCellEditingStyleInsert) {
        // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
    }   
}
*/

/*
// Override to support rearranging the table view.
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath
{
}
*/

/*
// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath
{
    // Return NO if you do not want the item to be re-orderable.
    return YES;
}
*/

-(void)configure:(NSString *)action
{
    // load gesture controller and set up
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSString *fullGesture = [[_passthrough objectForKey:@"Fingers"] stringByAppendingString:[_passthrough objectForKey:@"Gesture"]];
    NSString *fullGestureContinuous = [fullGesture stringByAppendingString:@"Continuous"];
    
    NSLog(@"defaults was %@",[defaults objectForKey:fullGesture]);
    // change the dictionary
    [defaults setObject:action forKey: fullGesture];
    if ([action isEqual:@"VolumeUp"] | [action isEqual:@"VolumeDown"] | [action isEqual:@"Rewind"] | [action isEqual:@"FastForward" ]) [defaults setObject:@"YES" forKey:fullGestureContinuous];
    else [defaults setObject:@"NO" forKey:fullGestureContinuous];


    NSLog(@"defaults is %@",[defaults objectForKey:fullGesture]);

    // save the dictionary
    [defaults synchronize];
    
    [self setUpActionChecks];
    NSLog(@"Configuring %@ fingers %@ (%@) to action %@",[_passthrough objectForKey:@"Fingers"],[_passthrough objectForKey:@"Gesture"],fullGesture,action);
}

-(void) tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    UITableViewCell *selection = [tableView cellForRowAtIndexPath:indexPath];
    if (selection == _Nothing) [self configure:@"Unassigned"];
    else if (selection == _Play) [self configure:@"Play"];
    else if (selection == _Pause) [self configure:@"Pause"];
    else if (selection == _PlayPause) [self configure:@"PlayPause"];
    else if (selection == _FastForward) [self configure:@"FastForward"];
    else if (selection == _Rewind) [self configure:@"Rewind"];
    else if (selection == _Next) [self configure:@"Next"];
    else if (selection == _Previous) [self configure:@"Previous"];
    else if (selection == _Restart) [self configure:@"Restart"];
    else if (selection == _RestartPrevious) [self configure:@"RestartPrevious"];
    else if (selection == _SongPicker) [self configure:@"SongPicker"];
    else if (selection == _VolumeUp) [self configure:@"VolumeUp"];
    else if (selection == _VolumeDown) [self configure:@"VolumeDown"];
    else if (selection == _startDefaultPlaylist) [self configure:@"StartDefaultPlaylist"];
    else if (selection == _Menu) [self configure:@"Menu"];
    else if (selection == _ResetGestureAssignments) [self initGestures];
    
    // if a theme cell was selected, set current theme
    else if (selection == _themeGreyOnWhite) { [defaults setObject:@"Grey on White" forKey:@"currentTheme"]; [self setUpThemeChecks]; }
    else if (selection == _themeGreyOnBlack) { [defaults setObject:@"Grey on Black" forKey:@"currentTheme"]; [self setUpThemeChecks]; }
    else if (selection == _themeLeaf) { [defaults setObject:@"Leaf" forKey:@"currentTheme"]; [self setUpThemeChecks]; }
    else if (selection == _themeOldWest) { [defaults setObject:@"Old West" forKey:@"currentTheme"]; [self setUpThemeChecks]; }
    else if (selection == _themeLavender) { [defaults setObject:@"Lavender" forKey:@"currentTheme"]; [self setUpThemeChecks]; }
    else if (selection == _themePeriwinkleBlue) { [defaults setObject:@"Periwinkle Blue" forKey:@"currentTheme"]; [self setUpThemeChecks]; }
    else if (selection == _themeBlush) { [defaults setObject:@"Blush" forKey:@"currentTheme"]; [self setUpThemeChecks]; }
    else if (selection == _themeHotDogStand) { [defaults setObject:@"Hot Dog Stand" forKey:@"currentTheme"]; [self setUpThemeChecks]; }
    else if (selection == _themeCustom) { [defaults setObject:@"Custom" forKey:@"currentTheme"]; [self setUpThemeChecks]; }
//    NSLog(@"Defaults are %@",[defaults objectForKey:@"currentTheme"]);
    
    /*
     consider:  PLAYALLSHUFFLE
     PLAYSHUFFLEDALBUMS
     PLAYARTISTSHUFFLED ... ARTISTSHUFFLEDBYALBUM
     GENIUSPLAYLIST
     RATESONG ?
     SHARE/TWEET/FB SONG ?
     SKIP FORWARD/BACK 5, 10, 20 SECONDS
     */
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
}

#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    settingsTableViewController *destination = [segue destinationViewController];

    // set up passthrough for subsequent view controller
    NSMutableDictionary *passthrough = [NSMutableDictionary dictionary];

    //preserve data from higher menus in passthrough dictionary
    if ([_passthrough objectForKey:@"Fingers"]!=nil) [passthrough setObject: [_passthrough objectForKey:@"Fingers"] forKey: @"Fingers"];
    if ([_passthrough objectForKey:@"Gesture"]!=nil) [passthrough setObject: [_passthrough objectForKey:@"Gesture"] forKey: @"Gesture"];
    
    // if finger menu segue, set Fingers key
    if ([[segue identifier] isEqual:@"1FingerMenu"]) [passthrough setObject: @"1" forKey: @"Fingers"];
    else if ([[segue identifier] isEqual:@"2FingerMenu"]) [passthrough setObject: @"2" forKey: @"Fingers"];
    else if ([[segue identifier] isEqual:@"3FingerMenu"]) [passthrough setObject: @"3" forKey: @"Fingers"];

    // if Gesture menu selection, set Gesture key
    if ([[segue identifier] isEqual:@"SwipeUp"]) [passthrough setObject: @"SwipeUp" forKey: @"Gesture"];
    else if ([[segue identifier] isEqual:@"SwipeDown"]) [passthrough setObject: @"SwipeDown" forKey: @"Gesture"];
    else if ([[segue identifier] isEqual:@"SwipeLeft"]) [passthrough setObject: @"SwipeLeft" forKey: @"Gesture"];
    else if ([[segue identifier] isEqual:@"SwipeRight"]) [passthrough setObject: @"SwipeRight" forKey: @"Gesture"];
    else if ([[segue identifier] isEqual:@"1Tap"]) [passthrough setObject: @"1Tap" forKey: @"Gesture"];
    else if ([[segue identifier] isEqual:@"2Tap"]) [passthrough setObject: @"2Tap" forKey: @"Gesture"];
    else if ([[segue identifier] isEqual:@"3Tap"]) [passthrough setObject: @"3Tap" forKey: @"Gesture"];
    else if ([[segue identifier] isEqual:@"4Tap"]) [passthrough setObject: @"4Tap" forKey: @"Gesture"];
    else if ([[segue identifier] isEqual:@"LongPress"]) [passthrough setObject: @"LongPress" forKey: @"Gesture"];

    // if a segue is not coded here you get a crash on the passthrough. so if we don't need to pass data, fill in "X"
    if ([[segue identifier] isEqual:@"quickStartGuide"]) {  [passthrough setObject: @"x" forKey: @"Fingers"]; [passthrough setObject: @"x" forKey: @"Gesture"]; }
    else if ([[segue identifier] isEqual:@"themeSettings"]) {  [passthrough setObject: @"x" forKey: @"Fingers"]; [passthrough setObject: @"x" forKey: @"Gesture"]; }
    else if ([[segue identifier] isEqual:@"RotationMenu"]) {  [passthrough setObject: @"x" forKey: @"Fingers"]; [passthrough setObject: @"x" forKey: @"Gesture"]; }

    
    // Pass the selected object to the new view controller and log.
    if ([[segue identifier] isEqual:@"quickStartGuide"]) { } else destination.passthrough = passthrough;
}

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    if(buttonIndex==0)
    {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        [gestureController initGestureAssignments];
        [defaults synchronize];
    }
    
}

- (void)initGestures {
    UIAlertView *updateAlert = [[UIAlertView alloc] initWithTitle: @"Confirm Reset" message: @"Are you sure you want to reset to defaults?" delegate: self cancelButtonTitle: @"YES"  otherButtonTitles:@"NO",nil];
    [updateAlert show];
}


// *** title display actions ********************************************************************
- (IBAction)artistAlignmentChanged:(id)sender {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    if (_artistAlignmentControl.selectedSegmentIndex == 0) {
        [defaults setObject:@"0" forKey:@"artistAlignment"];
    } else if(_artistAlignmentControl.selectedSegmentIndex == 1) {
        [defaults setObject:@"1" forKey:@"artistAlignment"];
    } else if(_artistAlignmentControl.selectedSegmentIndex == 2) {
        [defaults setObject:@"2" forKey:@"artistAlignment"];
    }
    [defaults synchronize];
}

- (IBAction)artistFontSizeSliderChanged:(id)sender {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    _artistFontSizeLabel.text = [NSString stringWithFormat:@"%i",(int)_artistFontSizeSlider.value];
    [defaults setObject:[NSNumber numberWithFloat:(int)_artistFontSizeSlider.value] forKey:@"artistFontSize"];
    [defaults synchronize];
    //= [UIFont systemFontOfSize:50];
}

- (IBAction)songAlignmentChanged:(id)sender {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    if (_songAlignmentControl.selectedSegmentIndex == 0) {
        [defaults setObject:@"0" forKey:@"songAlignment"];
    } else if(_songAlignmentControl.selectedSegmentIndex == 1) {
        [defaults setObject:@"1" forKey:@"songAlignment"];
    } else if(_songAlignmentControl.selectedSegmentIndex == 2) {
        [defaults setObject:@"2" forKey:@"songAlignment"];
    }
    [defaults synchronize];
}

- (IBAction)songFontSizeSliderChanged:(id)sender {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    _songFontSizeLabel.text = [NSString stringWithFormat:@"%i",(int)_songFontSizeSlider.value];
    [defaults setObject:[NSNumber numberWithFloat:(int)_songFontSizeSlider.value] forKey:@"songFontSize"];
    [defaults synchronize];
}

- (IBAction)albumAlignmentControl:(id)sender {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];

    if (_albumAlignmentControl.selectedSegmentIndex == 0) {
        [defaults setObject:@"0" forKey:@"albumAlignment"];
    } else if(_albumAlignmentControl.selectedSegmentIndex == 1) {
        [defaults setObject:@"1" forKey:@"albumAlignment"];
    } else if(_albumAlignmentControl.selectedSegmentIndex == 2) {
        [defaults setObject:@"2" forKey:@"albumAlignment"];
    }
    [defaults synchronize];
}

- (IBAction)albumFontSizeSliderChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    _albumFontSizeLabel.text = [NSString stringWithFormat:@"%i",(int)_albumFontSizeSlider.value];
    [defaults setObject:[NSNumber numberWithFloat:(int)_albumFontSizeSlider.value] forKey:@"albumFontSize"];
    [defaults synchronize];
}

// *** playlist switch actions ********************************************************************
- (IBAction)playlistShuffleChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_playlistShuffle.on) [defaults setObject:@"YES" forKey:@"shuffle"];
    else [defaults setObject:@"NO" forKey:@"shuffle"];
    [defaults synchronize];
    NSLog(@"shuffle: %hhd",_playlistShuffle.on);
}

- (IBAction)playlistRepeatChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_playlistRepeat.on) [defaults setObject:@"YES" forKey:@"repeat"];
    else [defaults setObject:@"NO" forKey:@"repeat"];
    NSLog(@"repeat: %hhd",_playlistRepeat.on);
    [defaults synchronize];
}

- (void) setThemeLabels {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];

//    NSLog(@"Invert is %@",[defaults objectForKey:@"themeInvert"]);
    
    NSArray *themecolors;
    UIColor *temp;
    UIColor *themebg;
    UIColor *themecolor;

    themecolors = [[gestureController themes] objectForKey:[defaults objectForKey:@"currentTheme"]];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themeSelectionPreview.backgroundColor = themebg;
    _themeSelectionPreviewLabel.textColor = themecolor;
    _themeSelectionPreviewTitle.textColor = themecolor;
    
    themecolors = [[gestureController themes] objectForKey:@"Grey on White"];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themeGreyOnWhite.backgroundColor = themebg;
    _themeGreyOnWhiteLabel.textColor = themecolor;
    
    themecolors = [[gestureController themes] objectForKey:@"Grey on Black"];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themeGreyOnBlack.backgroundColor = themebg;
    _themeGreyOnBlackLabel.textColor = themecolor;
    
    themecolors = [[gestureController themes] objectForKey:@"Leaf"];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themeLeaf.backgroundColor = themebg;
    _themeLeafLabel.textColor = themecolor;
    
    themecolors = [[gestureController themes] objectForKey:@"Old West"];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themeOldWest.backgroundColor = themebg;
    _themeOldWestLabel.textColor = themecolor;
    
    themecolors = [[gestureController themes] objectForKey:@"Lavender"];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themeLavender.backgroundColor = themebg;
    _themeLavenderLabel.textColor = themecolor;
    
    themecolors = [[gestureController themes] objectForKey:@"Blush"];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themeBlush.backgroundColor = themebg;
    _themeBlushLabel.textColor = themecolor;
    
    themecolors = [[gestureController themes] objectForKey:@"Periwinkle Blue"];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themePeriwinkleBlue.backgroundColor = themebg;
    _themePeriwinkleBlueLabel.textColor = themecolor;
    
    themecolors = [[gestureController themes] objectForKey:@"Hot Dog Stand"];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themeHotDogStand.backgroundColor = themebg;
    _themeHotDogStandLabel.textColor = themecolor;
    
    themecolors = [[gestureController themes] objectForKey:@"Custom"];
    themebg = [themecolors objectAtIndex:0]; themecolor = [themecolors objectAtIndex:1];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) { temp=themecolor; themecolor=themebg; themebg=temp; }
    _themeCustom.backgroundColor = themebg;
    _themeCustomLabel.textColor = themecolor;
    
}

- (IBAction)themeInvertChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_themeInvert.on) [defaults setObject:@"YES" forKey:@"themeInvert"];
    else [defaults setObject:@"NO" forKey:@"themeInvert"];
    [self setThemeLabels];
    [self setUpThemeChecks];
    [self updateCustomPreviews];
    NSLog(@"themeInvertChanged: %hhd",_themeInvert.on);
    [defaults synchronize];
}

- (void)updateCustomPreviews {
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    
        if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) {
                _customColorPreview.backgroundColor =
                [UIColor colorWithRed: (int)[[defaults objectForKey:@"customTextRed"] floatValue]/255.f
                                 green: (int)[[defaults objectForKey:@"customTextGreen"] floatValue]/255.f
                                  blue: (int)[[defaults objectForKey:@"customTextBlue"] floatValue]/255.f alpha:1];
                _customColorPreviewLabel.textColor =
                [UIColor colorWithRed: (int)[[defaults objectForKey:@"customBGRed"] floatValue]/255.f
                                 green: (int)[[defaults objectForKey:@"customBGGreen"] floatValue]/255.f
                                  blue: (int)[[defaults objectForKey:@"customBGBlue"] floatValue]/255.f alpha:1]; }
        else {
                _customColorPreviewLabel.textColor =
                [UIColor colorWithRed: (int)[[defaults objectForKey:@"customTextRed"] floatValue]/255.f
                                 green: (int)[[defaults objectForKey:@"customTextGreen"] floatValue]/255.f
                                  blue: (int)[[defaults objectForKey:@"customTextBlue"] floatValue]/255.f alpha:1];
                _customColorPreview.backgroundColor =
                [UIColor colorWithRed: (int)[[defaults objectForKey:@"customBGRed"] floatValue]/255.f
                                 green: (int)[[defaults objectForKey:@"customBGGreen"] floatValue]/255.f
                                  blue: (int)[[defaults objectForKey:@"customBGBlue"] floatValue]/255.f alpha:1]; }

        if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) {
                _customColorPreview2.backgroundColor =
                [UIColor colorWithRed: (int)[[defaults objectForKey:@"customTextRed"] floatValue]/255.f
                                 green: (int)[[defaults objectForKey:@"customTextGreen"] floatValue]/255.f
                                  blue: (int)[[defaults objectForKey:@"customTextBlue"] floatValue]/255.f alpha:1];
                _customColorPreviewLabel2.textColor =
                [UIColor colorWithRed: (int)[[defaults objectForKey:@"customBGRed"] floatValue]/255.f
                                 green: (int)[[defaults objectForKey:@"customBGGreen"] floatValue]/255.f
                                  blue: (int)[[defaults objectForKey:@"customBGBlue"] floatValue]/255.f alpha:1]; }
        else {
                _customColorPreviewLabel2.textColor =
                [UIColor colorWithRed: (int)[[defaults objectForKey:@"customTextRed"] floatValue]/255.f
                                 green: (int)[[defaults objectForKey:@"customTextGreen"] floatValue]/255.f
                                  blue: (int)[[defaults objectForKey:@"customTextBlue"] floatValue]/255.f alpha:1];
                _customColorPreview2.backgroundColor =
                [UIColor colorWithRed: (int)[[defaults objectForKey:@"customBGRed"] floatValue]/255.f
                                 green: (int)[[defaults objectForKey:@"customBGGreen"] floatValue]/255.f
                                  blue: (int)[[defaults objectForKey:@"customBGBlue"] floatValue]/255.f alpha:1]; }
    [[gestureController themes] setObject:[NSArray arrayWithObjects:
                                           [UIColor colorWithRed: (int)[[defaults objectForKey:@"customBGRed"] floatValue]/255.f
                                                           green: (int)[[defaults objectForKey:@"customBGGreen"] floatValue]/255.f
                                                            blue: (int)[[defaults objectForKey:@"customBGBlue"] floatValue]/255.f   alpha:1],
                                           [UIColor colorWithRed: (int)[[defaults objectForKey:@"customTextRed"] floatValue]/255.f
                                                           green: (int)[[defaults objectForKey:@"customTextGreen"] floatValue]/255.f
                                                            blue: (int)[[defaults objectForKey:@"customTextBlue"] floatValue]/255.f alpha:1],nil] forKey:@"Custom"];
    [gestureController saveThemes];
}

- (IBAction)textRedChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[NSNumber numberWithFloat:(int)_textRedSlider.value] forKey:@"customTextRed"];
    [defaults synchronize];
    [self updateCustomPreviews];
}

- (IBAction)textGreenChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[NSNumber numberWithFloat:(int)_textGreenSlider.value] forKey:@"customTextGreen"];
    [defaults synchronize];    
    [self updateCustomPreviews];
}

- (IBAction)textBlueChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[NSNumber numberWithFloat:(int)_textBlueSlider.value] forKey:@"customTextBlue"];
    [defaults synchronize];
    [self updateCustomPreviews];
}

- (IBAction)bgRedChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[NSNumber numberWithFloat:(int)_bgRedSlider.value] forKey:@"customBGRed"];
    [defaults synchronize];
    [self updateCustomPreviews];
}

- (IBAction)bgGreenChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[NSNumber numberWithFloat:(int)_bgGreenSlider.value] forKey:@"customBGGreen"];    
    [defaults synchronize];
    [self updateCustomPreviews];
}

- (IBAction)bgBlueChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[NSNumber numberWithFloat:(int)_bgBlueSlider.value] forKey:@"customBGBlue"];
    [defaults synchronize];
    [self updateCustomPreviews];
}

- (void)setUpThemeChecks {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];

    int index = 0;
    _themeGreyOnWhiteCheck.textColor = [[[gestureController themes] objectForKey:@"Grey on White"] objectAtIndex:index];
    _themeGreyOnBlackCheck.textColor = [[[gestureController themes] objectForKey:@"Grey on Black"] objectAtIndex:index];
    _themeLavenderCheck.textColor = [[[gestureController themes] objectForKey:@"Lavender"] objectAtIndex:index];
    _themeBlushCheck.textColor = [[[gestureController themes] objectForKey:@"Blush"] objectAtIndex:index];
    _themeLeafCheck.textColor = [[[gestureController themes] objectForKey:@"Leaf"] objectAtIndex:index];
    _themeOldWestCheck.textColor = [[[gestureController themes] objectForKey:@"Old West"] objectAtIndex:index];
    _themePeriwinkleBlueCheck.textColor = [[[gestureController themes] objectForKey:@"Periwinkle Blue"] objectAtIndex:index];
    _themeHotDogStandCheck.textColor = [[[gestureController themes] objectForKey:@"Hot Dog Stand"] objectAtIndex:index];
    _themeCustomCheck.textColor = [[[gestureController themes] objectForKey:@"Custom"] objectAtIndex:index];
    if ([[defaults objectForKey:@"themeInvert"] isEqual:@"YES"]) {
        index = 0;
    } else index = 1;
    if ([[defaults objectForKey:@"currentTheme"] isEqual: @"Grey on White"]) _themeGreyOnWhiteCheck.textColor = [[[gestureController themes] objectForKey:@"Grey on White"] objectAtIndex:index];
    if ([[defaults objectForKey:@"currentTheme"] isEqual: @"Grey on Black"]) _themeGreyOnBlackCheck.textColor = [[[gestureController themes] objectForKey:@"Grey on Black"] objectAtIndex:index];
    if ([[defaults objectForKey:@"currentTheme"] isEqual: @"Lavender"]) _themeLavenderCheck.textColor = [[[gestureController themes] objectForKey:@"Lavender"] objectAtIndex:index];
    if ([[defaults objectForKey:@"currentTheme"] isEqual: @"Blush"]) _themeBlushCheck.textColor = [[[gestureController themes] objectForKey:@"Blush"] objectAtIndex:index];
    if ([[defaults objectForKey:@"currentTheme"] isEqual: @"Leaf"]) _themeLeafCheck.textColor = [[[gestureController themes] objectForKey:@"Leaf"] objectAtIndex:index];
    if ([[defaults objectForKey:@"currentTheme"] isEqual: @"Old West"]) _themeOldWestCheck.textColor = [[[gestureController themes] objectForKey:@"Old West"] objectAtIndex:index];
    if ([[defaults objectForKey:@"currentTheme"] isEqual: @"Periwinkle Blue"]) _themePeriwinkleBlueCheck.textColor = [[[gestureController themes] objectForKey:@"Periwinkle Blue"] objectAtIndex:index];
    if ([[defaults objectForKey:@"currentTheme"] isEqual: @"Hot Dog Stand"]) _themeHotDogStandCheck.textColor = [[[gestureController themes] objectForKey:@"Hot Dog Stand"] objectAtIndex:index];
    if ([[defaults objectForKey:@"currentTheme"] isEqual: @"Custom"]) _themeCustomCheck.textColor = [[[gestureController themes] objectForKey:@"Custom"] objectAtIndex:index];
}



- (void)setUpActionChecks {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (([_passthrough objectForKey:@"Fingers"]!=NULL) & ([_passthrough objectForKey:@"Gesture"]!=NULL)) {
        NSString *fullGesture = [[_passthrough objectForKey:@"Fingers"] stringByAppendingString:[_passthrough objectForKey:@"Gesture"]];
        NSLog(@"fullGesture is %@",[defaults objectForKey:fullGesture]);
        _nothingCheck.textColor = [UIColor whiteColor];
        _menuCheck.textColor = [UIColor whiteColor];
        _playCheck.textColor = [UIColor whiteColor];
        _pauseCheck.textColor = [UIColor whiteColor];
        _playPauseCheck.textColor = [UIColor whiteColor];
        _nextCheck.textColor = [UIColor whiteColor];
        _previousCheck.textColor = [UIColor whiteColor];
        _restartCheck.textColor = [UIColor whiteColor];
        _restartPreviousCheck.textColor = [UIColor whiteColor];
        _rewindCheck.textColor = [UIColor whiteColor];
        _fastForwardCheck.textColor = [UIColor whiteColor];
        _volumeUpCheck.textColor = [UIColor whiteColor];
        _volumeDownCheck.textColor = [UIColor whiteColor];
        _startDefaultPlaylistCheck.textColor = [UIColor whiteColor];
        _songPickerCheck.textColor = [UIColor whiteColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"Unassigned"]) _nothingCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"Menu"]) _menuCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"Play"]) _playCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"Pause"]) _pauseCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"PlayPause"]) _playPauseCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"Next"]) _nextCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"Previous"]) _previousCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"Restart"]) _restartCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"RestartPrevious"]) _restartPreviousCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"Rewind"]) _rewindCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"FastForward"]) _fastForwardCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"VolumeUp"]) _volumeUpCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"VolumeDown"]) _volumeDownCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"StartDefaultPlaylist"]) _startDefaultPlaylistCheck.textColor = [UIColor blackColor];
        if ([[defaults objectForKey:fullGesture] isEqual: @"SongPicker"]) _songPickerCheck.textColor = [UIColor blackColor];
    }
}
/*
 - (IBAction)albumFontSizeSliderChanged:(id)sender {
 NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
 _albumFontSizeLabel.text = [NSString stringWithFormat:@"%i",(int)_albumFontSizeSlider.value];
 [defaults setObject:[NSNumber numberWithFloat:(int)_albumFontSizeSlider.value] forKey:@"albumFontSize"];
 [defaults synchronize];
 }
 
 // *** playlist switch actions ********************************************************************
 - (IBAction)playlistShuffleChanged:(id)sender {
 NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
 if (_playlistShuffle.on) [defaults setObject:@"YES" forKey:@"shuffle"];
 else [defaults setObject:@"NO" forKey:@"shuffle"];
 [defaults synchronize];
 NSLog(@"shuffle: %hhd",_playlistShuffle.on);
 }
*/
- (IBAction)titleShrinkInPortraitChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_titleShrinkInPortrait.on) [defaults setObject:@"YES" forKey:@"titleShrinkInPortrait"];
    else [defaults setObject:@"NO" forKey:@"titleShrinkInPortrait"];
    [defaults synchronize];
}

- (IBAction)titleShrinkToFitChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_titleShrinkLong.on) [defaults setObject:@"YES" forKey:@"titleShrinkLong"];
    else [defaults setObject:@"NO" forKey:@"titleShrinkLong"];
    [defaults synchronize];
}

- (IBAction)titleShrinkMinimumChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    _titleShrinkMinimumLabel.text = [NSString stringWithFormat:@"%i",(int)_titleShrinkMinimumSlider.value];
    [defaults setObject:[NSNumber numberWithFloat:(int)_titleShrinkMinimumSlider.value] forKey:@"minimumFontSize"];
    [defaults synchronize];
}

- (IBAction)volumeSensitivityChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[NSNumber numberWithFloat:_volumeSensitivitySlider.value] forKey:@"volumeSensitivity"];
    [defaults synchronize];
}

- (IBAction)seekSensitivityChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[NSNumber numberWithFloat:_seekSensitivitySlider.value] forKey:@"seekSensitivity"];
    [defaults synchronize];
}

- (IBAction)rotationPortraitChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_rotationPortrait.on) [defaults setObject:@"YES" forKey:@"RotationPortrait"];
    else [defaults setObject:@"NO" forKey:@"RotationPortrait"];
    [defaults synchronize];
}

- (IBAction)rotationClockwiseChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_rotationClockwise.on) [defaults setObject:@"YES" forKey:@"RotationClockwise"];
    else [defaults setObject:@"NO" forKey:@"RotationClockwise"];
    [defaults synchronize];
}

- (IBAction)rotationAntiClockwiseChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_rotationAntiClockwise.on) [defaults setObject:@"YES" forKey:@"RotationAntiClockwise"];
    else [defaults setObject:@"NO" forKey:@"RotationAntiClockwise"];
    [defaults synchronize];
}

- (IBAction)rotationInvertedChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_rotationInverted.on) [defaults setObject:@"YES" forKey:@"RotationInverted"];
    else [defaults setObject:@"NO" forKey:@"RotationInverted"];
    [defaults synchronize];
}

- (IBAction)showStatusBarChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_showStatusBar.on) [defaults setObject:@"YES" forKey:@"ShowStatusBar"];
    else [defaults setObject:@"NO" forKey:@"ShowStatusBar"];
    [defaults synchronize];
}

- (IBAction)HUDTypeChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    
    if (_HUDType.selectedSegmentIndex == 0) {
        [defaults setObject:@"0" forKey:@"HUDType"];
    } else if(_HUDType.selectedSegmentIndex == 1) {
        [defaults setObject:@"1" forKey:@"HUDType"];
    } else if(_HUDType.selectedSegmentIndex == 2) {
        [defaults setObject:@"2" forKey:@"HUDType"];
    } else if(_HUDType.selectedSegmentIndex == 3) {
        [defaults setObject:@"3" forKey:@"HUDType"];
    } else if(_HUDType.selectedSegmentIndex == 4) {
        [defaults setObject:@"4" forKey:@"HUDType"];
    }
    [defaults synchronize];
}

- (IBAction)scrubHUDTypeChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    
    if (_ScrubHUDType.selectedSegmentIndex == 0) {
        [defaults setObject:@"0" forKey:@"ScrubHUDType"];
    } else if(_ScrubHUDType.selectedSegmentIndex == 1) {
        [defaults setObject:@"1" forKey:@"ScrubHUDType"];
    } else if(_ScrubHUDType.selectedSegmentIndex == 2) {
        [defaults setObject:@"2" forKey:@"ScrubHUDType"];
    } else if(_ScrubHUDType.selectedSegmentIndex == 3) {
        [defaults setObject:@"3" forKey:@"ScrubHUDType"];
    }
    [defaults synchronize];
}

- (IBAction)volumeAlwaysOnChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_volumeAlwaysOn.on) [defaults setObject:@"YES" forKey:@"VolumeAlwaysOn"];
    else [defaults setObject:@"NO" forKey:@"VolumeAlwaysOn"];
    [defaults synchronize];

}

- (IBAction)playOnLaunchChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_playOnLaunch.on) [defaults setObject:@"YES" forKey:@"PlayOnLaunch"];
    else [defaults setObject:@"NO" forKey:@"PlayOnLaunch"];
    [defaults synchronize];
}

- (IBAction)pauseOnExitChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_pauseOnExit.on) [defaults setObject:@"YES" forKey:@"PauseOnExit"];
    else [defaults setObject:@"NO" forKey:@"PauseOnExit"];
    [defaults synchronize];
}

- (IBAction)invertAtNightChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_invertAtNight.on) [defaults setObject:@"YES" forKey:@"InvertAtNight"];
    else [defaults setObject:@"NO" forKey:@"InvertAtNight"];
    [defaults synchronize];
}

- (IBAction)dimAtNightChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if (_dimAtNightSwitch.on) [defaults setObject:@"YES" forKey:@"DimAtNight"];
    else [defaults setObject:@"NO" forKey:@"DimAtNight"];
    [defaults synchronize];
}

- (IBAction)sunRiseChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject:[NSNumber numberWithFloat:(int)_sunRiseSlider.value] forKey:@"SunRiseHour"];
    NSString *labelText;
    if ((int)[[defaults objectForKey:@"SunRiseHour"] floatValue] > 12) {
        labelText = [[NSString stringWithFormat:@"%i",(int)([[defaults objectForKey:@"SunRiseHour"] floatValue]-12)] stringByAppendingString:@" pm"];
    } else if ((int)[[defaults objectForKey:@"SunRiseHour"] floatValue] == 12)
        labelText = [[NSString stringWithFormat:@"%i",(int)([[defaults objectForKey:@"SunRiseHour"] floatValue])] stringByAppendingString:@" pm"];
    else labelText = [[NSString stringWithFormat:@"%i",(int)[[defaults objectForKey:@"SunRiseHour"] floatValue]] stringByAppendingString:@" am"];
    _sunRiseLabel.text = labelText;
    [defaults synchronize];
}

- (IBAction)sunSetChanged:(id)sender {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
//    _sunSetLabel.text = [NSString stringWithFormat:@"%i",(int)_sunSetSlider.value];
    [defaults setObject:[NSNumber numberWithFloat:(int)_sunSetSlider.value] forKey:@"SunSetHour"];
    NSString *labelText;
    if ((int)[[defaults objectForKey:@"SunSetHour"] floatValue] > 12) {
        labelText = [[NSString stringWithFormat:@"%i",(int)([[defaults objectForKey:@"SunSetHour"] floatValue]-12)] stringByAppendingString:@" pm"];
    } else if ((int)[[defaults objectForKey:@"SunSetHour"] floatValue] == 12)
        labelText = [[NSString stringWithFormat:@"%i",(int)([[defaults objectForKey:@"SunSetHour"] floatValue])] stringByAppendingString:@" pm"];
    else labelText = [[NSString stringWithFormat:@"%i",(int)[[defaults objectForKey:@"SunSetHour"] floatValue]] stringByAppendingString:@" am"];
    _sunSetLabel.text = labelText;
    [defaults synchronize];
}


@end