//
//  contactsTableViewController.m
//  Traveling Tunes
//
//  Created by buck on 4/27/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "contactsTableViewController.h"
#import "contactsTableViewCell.h"
#import "ttunesViewController.h"

@interface contactsTableViewController ()

@end

@implementation contactsTableViewController

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
    
    _names = [[NSMutableArray alloc] init];
    _addresses = [[NSMutableArray alloc] init];
    _temp = [[NSMutableArray alloc] init];

    [self listContacts];
    
    
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
    
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
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
    return [_names count];
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [textField resignFirstResponder];
    
    [defaults setObject:textField.text forKey:@"destinationAddress"];
    [defaults synchronize];
    NSIndexPath *indexPath = [NSIndexPath indexPathForRow:0 inSection:0];
    [self.tableView selectRowAtIndexPath:indexPath animated:YES scrollPosition:UITableViewScrollPositionNone];
    [self tableView:self.tableView didSelectRowAtIndexPath:indexPath];
    return YES;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSString *cellType = @"textEntryCell";
    if ([indexPath row]>0) cellType = @"contactCell";
        contactsTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:cellType forIndexPath:indexPath];
        if (cell == nil) {
            cell = [[contactsTableViewCell alloc]
                    initWithStyle:UITableViewCellStyleDefault
                    reuseIdentifier:cellType];
        }
    NSLog(@"%lu names, %lu addresses, outputting row %ld",(unsigned long)[_names count],(unsigned long)[_addresses count],(long)[indexPath row]+1);
    
    if ([indexPath row]==0) {
//        [cell.textField bind:@"value" toObject:self withKeyPath:@"self.textFieldString" options:nil];
        cell.textField.delegate = self;

    }
    if ([indexPath row]>0) {
    // Configure the cell...
        cell.nameLabel.frame = CGRectMake(20,10,self.view.bounds.size.width-20,20);
        [cell.nameLabel setTextColor:[UIColor blackColor]];
        [cell.nameLabel setBackgroundColor:[UIColor clearColor]];
        [cell.nameLabel setFont:[UIFont systemFontOfSize: 18.0f]];
        cell.nameLabel.text = [_names objectAtIndex:[indexPath row]+1];
    
        [cell.addressLabel setTextColor:[UIColor lightGrayColor]];
        [cell.addressLabel setBackgroundColor:[UIColor clearColor]];
        [cell.addressLabel setFont:[UIFont systemFontOfSize: 18.0f]];
        cell.addressLabel.frame = CGRectMake([cell.nameLabel.text sizeWithFont:[UIFont systemFontOfSize:18]].width+30, 10, (self.view.bounds.size.width-[cell.nameLabel.text sizeWithFont:[UIFont systemFontOfSize:18]].width)-40, 20);
        NSString *fullAddress = [NSString stringWithFormat:@"%@",[[_addresses objectAtIndex:[indexPath row]+1] objectForKey:@"Street"]];
        if ([[_addresses objectAtIndex:[indexPath row]+1] objectForKey:@"City"])
            fullAddress = [fullAddress stringByAppendingString:[NSString stringWithFormat:@", %@",[[_addresses objectAtIndex:[indexPath row]+1] objectForKey:@"City"]]];
        if ([[_addresses objectAtIndex:[indexPath row]+1] objectForKey:@"State"])
            fullAddress = [fullAddress stringByAppendingString: [NSString stringWithFormat:@", %@",[[_addresses objectAtIndex:[indexPath row]+1] objectForKey:@"State"]]];
        cell.addressLabel.text = fullAddress;
    }
    
    return cell;
}

-(void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    //    gestureAssignmentController *gestureController = [[gestureAssignmentController alloc] init];
    contactsTableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
    NSString *cellText = cell.addressLabel.text;
    
    // if called from "pick contact" action, set navigation destination
    switch ([indexPath row]) {
        case 0:
            [defaults setObject:cell.textField.text forKey:@"destinationAddress"];
            [defaults synchronize];
            break;
        default:
            if ([[_passthrough objectForKey:@"sender"] isEqual:@"openContacts"]) {
                NSLog(@"called from view controller");
                [defaults setObject:cell.addressLabel.text forKey:@"destinationAddress"];
                [defaults synchronize];
            } else { // if called from settings pane, set home to new address
                [defaults setObject:cellText forKey:@"homeAddress"];
                NSLog(@"set home to %@",[defaults objectForKey:@"homeAddress"]);
                [defaults synchronize];
            }
            break;
    }
    [self.navigationController popViewControllerAnimated:YES];
}

-(void)listContacts {
    int current = 0;
    __block BOOL userDidGrantAddressBookAccess;
    CFErrorRef addressBookError = NULL;
    ABAddressBookRef addressBook = ABAddressBookCreateWithOptions(NULL, NULL);

    
    if ( ABAddressBookGetAuthorizationStatus() == kABAuthorizationStatusNotDetermined ||
        ABAddressBookGetAuthorizationStatus() == kABAuthorizationStatusAuthorized )
    {
        addressBook = ABAddressBookCreateWithOptions(NULL, &addressBookError);
        dispatch_semaphore_t sema = dispatch_semaphore_create(0);
        ABAddressBookRequestAccessWithCompletion(addressBook, ^(bool granted, CFErrorRef error){
            userDidGrantAddressBookAccess = granted;
            dispatch_semaphore_signal(sema);
        });
        dispatch_semaphore_wait(sema, DISPATCH_TIME_FOREVER);
    }
    else
    {
        if ( ABAddressBookGetAuthorizationStatus() == kABAuthorizationStatusDenied ||
            ABAddressBookGetAuthorizationStatus() == kABAuthorizationStatusRestricted )
        {
            // Display an error.
        }
    }
    if (addressBook != nil)
    {
#ifdef DEBUG
        NSLog(@"Successfully accessed the address book.");
#endif
        
        CFArrayRef allPeople = ABAddressBookCopyArrayOfAllPeople(addressBook);
        CFIndex nPeople= ABAddressBookGetPersonCount(addressBook);
        
        NSUInteger peopleCounter = 0;
        for (peopleCounter = 0;peopleCounter < nPeople; peopleCounter++)
        {
            
            ABRecordRef thisPerson = CFArrayGetValueAtIndex(allPeople,peopleCounter);
            NSString *contactFirstLast = @"";
            
            if ((ABRecordCopyValue(thisPerson, kABPersonFirstNameProperty)!=NULL)|(ABRecordCopyValue(thisPerson, kABPersonLastNameProperty)!=NULL)) {
                if ((ABRecordCopyValue(thisPerson, kABPersonFirstNameProperty)!=NULL)&(ABRecordCopyValue(thisPerson, kABPersonLastNameProperty)!=NULL))
                    contactFirstLast = [NSString stringWithFormat:@"%@, %@",ABRecordCopyValue(thisPerson, kABPersonFirstNameProperty), ABRecordCopyValue(thisPerson,kABPersonLastNameProperty)];
                else if (ABRecordCopyValue(thisPerson, kABPersonFirstNameProperty)!=NULL)
                    contactFirstLast = [NSString stringWithFormat:@"%@",ABRecordCopyValue(thisPerson, kABPersonFirstNameProperty)];
                else if (ABRecordCopyValue(thisPerson, kABPersonFirstNameProperty)!=NULL)
                    contactFirstLast = [NSString stringWithFormat:@"%@",ABRecordCopyValue(thisPerson, kABPersonLastNameProperty)];
            }
            else contactFirstLast = [NSString stringWithFormat:@"%@",ABRecordCopyValue(thisPerson, kABPersonOrganizationProperty)];
            
            ABMultiValueRef personAddresses = ABRecordCopyValue(thisPerson,kABPersonAddressProperty);
            NSString *firstAddress = (__bridge_transfer NSString*) ABMultiValueCopyValueAtIndex(personAddresses, 0);
            
            // now that we have a name and address, only add to the list if the address is not null
            if ((firstAddress!=NULL)&(contactFirstLast!=NULL)) {
                [_temp addObject:firstAddress];
                // filter out people with addresses but no street address
                if ([[_temp objectAtIndex:current] objectForKey:@"Street"]) {
                    [_names addObject:contactFirstLast];
                    [_addresses addObject:firstAddress];
                    current++;
#ifdef DEBUG
//                    NSLog(@"%lu: %@ at %@",(unsigned long)peopleCounter,contactFirstLast,firstAddress);
#endif
                }
            }

        }
    }

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
