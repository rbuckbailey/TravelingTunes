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
}

- (void)viewDidLoad
{
    [super viewDidLoad];
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

-(void) configure:(NSString *)action
{
    // load gesture controller and set up
    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    NSMutableDictionary *assignments = [gestureController assignments];
    NSString *fullGesture = [[_passthrough objectForKey:@"Fingers"] stringByAppendingString:[_passthrough objectForKey:@"Gesture"]];

    // change the dictionary
    [assignments setObject:action forKey: fullGesture];
   
    // save the dictionary
    [gestureController saveGestureAssignments];
    
    NSLog(@"Configuring %@ fingers %@ (%@) to action %@",[_passthrough objectForKey:@"Fingers"],[_passthrough objectForKey:@"Gesture"],fullGesture,action);
}

-(void) tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
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
    else if (selection == _Menu) [self configure:@"Menu"];
    else if (selection == _ResetGestureAssignments) [self resetGestureSettings];
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

    // if a segue is not coded here you get a crash on this passthrough. so if we don't need to pass data, fill in "X"
    if ([[segue identifier] isEqual:@"quickStartGuide"]) {  [passthrough setObject: @"x" forKey: @"Fingers"]; [passthrough setObject: @"x" forKey: @"Gesture"]; }

    
    // Pass the selected object to the new view controller and log.
    destination.passthrough = passthrough;
}

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    if(buttonIndex==0)
    {
        gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
        [gestureController resetAssignments];
        [gestureController saveGestureAssignments];
    }
    
}

- (void)resetGestureSettings {
    UIAlertView *updateAlert = [[UIAlertView alloc] initWithTitle: @"Confirm Reset" message: @"Are you sure you want to reset to defaults?" delegate: self cancelButtonTitle: @"YES"  otherButtonTitles:@"NO",nil];
    [updateAlert show];
}


@end
