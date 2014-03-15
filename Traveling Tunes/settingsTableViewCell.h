//
//  settingsTableViewCell.h
//  Traveling Tunes
//
//  Created by buck on 3/14/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface settingsTableViewCell : UITableViewCell
@property (weak, nonatomic) IBOutlet UIButton *submenuButton;
@property (weak, nonatomic) IBOutlet UILabel *switchLabel;
@property (weak, nonatomic) IBOutlet UISwitch *switchToggle;
@property (weak, nonatomic) IBOutlet UILabel *slideLabelA;
@property (weak, nonatomic) IBOutlet UILabel *slideLabelB;
@property (weak, nonatomic) IBOutlet UISlider *slideSlide;

@end
