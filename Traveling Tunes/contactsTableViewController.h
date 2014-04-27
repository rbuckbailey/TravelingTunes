//
//  contactsTableViewController.h
//  Traveling Tunes
//
//  Created by buck on 4/27/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AddressBook/AddressBook.h>

@interface contactsTableViewController : UITableViewController
@property NSMutableDictionary *passthrough;
@property NSMutableArray *names;// = [[NSMutableArray alloc] init];
@property NSMutableArray *addresses;// = [[NSMutableArray alloc] init];
@property NSMutableArray *temp;// = [[NSMutableArray alloc] init];

@end
