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
@property (nonatomic, strong) NSArray *topLevel;
@property (nonatomic, strong) NSArray *controls;
@property (nonatomic, strong) NSArray *display;
@property (nonatomic, strong) NSArray *launchquit;
@property (nonatomic, strong) NSArray *playlists;
@property (weak, nonatomic) IBOutlet UITableViewCell *Play;
@property (atomic) int menuLevel;
@end

@implementation settingsTableViewController

-(void)viewWillAppear:(BOOL)animated
{
    [self.navigationController setNavigationBarHidden:NO];
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    _topLevel = @[@"Controls",@"Display",@"Launch/Quit Options",@"Playlists"];
    _controls = @[@"1 Finger Gestures",@"2 Finger Gestures", @"3 Finger Gestures",@"Swipe Sensitivity",@"Volume Sensitivity"];
    _display = @[@"Auto Lock/Dim Settings",@"Song Headers",@"Rotation Styles",@"Layout Styles"];
    _launchquit = @[@"Play on Launch",@"Pause on Quit"];
    _playlists = @[@"Default Playlist",@"Shuffle Mode",@"Repeat Mode"];
    
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
    
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
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
    switch (_menuLevel) {
        case MENU_TOPLEVEL: _menuLevel = MENU_CONTROLS; break;
        case MENU_CONTROLS: _menuLevel = MENU_DISPLAY; break;
        case MENU_DISPLAY: _menuLevel = MENU_TOPLEVEL; break;
    }
    [self performSegueWithIdentifier: @"nextSettings" sender: self];

//     [self performSegueWithIdentifier:@"settingsControls" sender:self];
/*
     UIStoryboard *mainStoryboard = [UIStoryboard storyboardWithName:@"settingsTable"
     bundle: nil];
     // Imagine it's called MyCustomTableVC
     settingsTableViewController *newVC = (settingsTableViewController*)[mainStoryboard
     instantiateViewControllerWithIdentifier: @"settingsTableViewController"];
     newVC.menuLevel = MENU_CONTROLS; //[self.model objectAtIndex:indexPath.row];
     [self.navigationController pushViewController:newVC animated:YES];
*/
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


#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
    if ([[segue identifier] isEqualToString:@"SwipeUp"]) {
        
        //Destination *detailViewController = [segue destinationViewController];
        
        //detailViewController.infoRequest = @"SwipeUp";
        NSLog(@"SwipeUp");
    }
    if ([[segue identifier] isEqualToString:@"SwipeUp"]) {
        
        //Destination *detailViewController = [segue destinationViewController];
        
        //detailViewController.infoRequest = @"SwipeUp";
        NSLog(@"SwipeUp");
    }
}


@end
