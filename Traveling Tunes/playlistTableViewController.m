//
//  playlistTableViewController.m
//  Traveling Tunes
//
//  Created by buck on 3/25/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "playlistTableViewController.h"
#import "playlistTableViewCell.h"
#import "gestureAssignmentController.h"


MPMusicPlayerController*        mediaPlayer;

@interface playlistTableViewController ()

@end

@implementation playlistTableViewController

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
 /*
    UIBarButtonItem *rightButton = [[UIBarButtonItem alloc] initWithTitle:@"Done"
                                                                    style:UIBarButtonItemStyleDone target:self action:@selector(popToRoot)];
    self.navigationItem.rightBarButtonItem = rightButton;
*/
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
    
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
    
    [self listPlaylists];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    // Return the number of sections.
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    // Return the number of rows in the section.
    return [_playlists count]+2;
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *CellIdentifier = @"playlist";
    playlistTableViewCell *cell = [tableView
                              dequeueReusableCellWithIdentifier:CellIdentifier];
    if (cell == nil) {
        cell = [[playlistTableViewCell alloc]
                initWithStyle:UITableViewCellStyleDefault
                reuseIdentifier:CellIdentifier];
    }
    // Configure the cell...
    switch ([indexPath row]) {
        case 0: cell.playlistLabel.text = @"All Songs, Shuffled";
            break;
        case 1: cell.playlistLabel.text = @"All Songs by Album"; break;
        default:
            cell.playlistLabel.text = [[_playlists objectAtIndex:[indexPath row]-2] valueForProperty: MPMediaPlaylistPropertyName];
            break;
    }
    return cell;
}

- (void) listPlaylists {
    MPMediaQuery *query = [MPMediaQuery playlistsQuery];
    _playlists = [query collections];
    
    for(int i = 0; i < [_playlists count]; i++)
    {
//        NSLog(@"Playlist : %@", [[_playlists objectAtIndex:i] valueForProperty: MPMediaPlaylistPropertyName]);
    }
}

-(void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    //    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
        playlistTableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
    NSString *cellText = cell.playlistLabel.text;

//    NSLog(@"%@",cell.playlistLabel.text);

    switch ([indexPath row]) {
        case 0: [defaults setObject:@"All Songs, Shuffled" forKey:@"playlist"]; break;
        case 1: [defaults setObject:@"All Songs by Album" forKey:@"playlist"]; break;
        default:
            [defaults setObject:cellText forKey:@"playlist"]; break;
    }
//    NSLog(@"set playlist to %@",[defaults objectForKey:@"playlist"]);
    [defaults synchronize];
    [self.navigationController popViewControllerAnimated:YES];
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

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
