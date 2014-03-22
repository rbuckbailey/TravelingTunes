//
//  settingsTableViewController.m
//  Traveling Tunes
//
//  Created by buck on 3/14/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "settingsTableViewController.h"
#import "settingsTableViewCell.h"

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
    NSLog(@"%@",_passthrough);
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

-(void) tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
    NSLog(@"Selected %@",cell.textLabel.text);
    NSLog(@"tag %i",cell.tag);
    
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
}

#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    settingsTableViewController *destination = [segue destinationViewController];

    // set up data to pass through based on segue identifier
    NSMutableDictionary *passthrough = [NSMutableDictionary dictionary];
    //this is hard coded but should work based on segue identifier

    //preserve data from higher menus in passthrough dictionary
    if ([_passthrough objectForKey:@"Fingers"]!=nil) [passthrough setObject: [_passthrough objectForKey:@"Fingers"] forKey: @"Fingers"];
    if ([_passthrough objectForKey:@"Gesture"]!=nil) [passthrough setObject: [_passthrough objectForKey:@"Gesture"] forKey: @"Gesture"];
    
    // if finger menu segue, set Fingers key
    if ([[segue identifier] isEqual:@"1FingerMenu"]) [passthrough setObject: @"1" forKey: @"Fingers"];
    if ([[segue identifier] isEqual:@"2FingerMenu"]) [passthrough setObject: @"2" forKey: @"Fingers"];
    if ([[segue identifier] isEqual:@"3FingerMenu"]) [passthrough setObject: @"3" forKey: @"Fingers"];

    // if Gesture menu selection, set Gesture key
    if ([[segue identifier] isEqual:@"SwipeUp"]) [passthrough setObject: @"SwipeUp" forKey: @"Gesture"];
    if ([[segue identifier] isEqual:@"SwipeDown"]) [passthrough setObject: @"SwipeDown" forKey: @"Gesture"];
    if ([[segue identifier] isEqual:@"SwipeLeft"]) [passthrough setObject: @"SwipeLeft" forKey: @"Gesture"];
    if ([[segue identifier] isEqual:@"SwipeRight"]) [passthrough setObject: @"SwipeRight" forKey: @"Gesture"];
    if ([[segue identifier] isEqual:@"1Tap"]) [passthrough setObject: @"1Tap" forKey: @"Gesture"];
    if ([[segue identifier] isEqual:@"2Tap"]) [passthrough setObject: @"2Tap" forKey: @"Gesture"];
    if ([[segue identifier] isEqual:@"3Tap"]) [passthrough setObject: @"3Tap" forKey: @"Gesture"];
    if ([[segue identifier] isEqual:@"4Tap"]) [passthrough setObject: @"4Tap" forKey: @"Gesture"];
    if ([[segue identifier] isEqual:@"LongPress"]) [passthrough setObject: @"LongPress" forKey: @"Gesture"];

    // Pass the selected object to the new view controller and log.
    destination.passthrough = passthrough;
    //NSLog(@"Sending %@ to destination view.",[segue identifier]);
}


@end
