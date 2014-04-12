//
//  quickstartViewController.h
//  Traveling Tunes
//
//  Created by buck on 4/10/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface quickstartViewController : UIViewController 

@property (strong, nonatomic) id dataObject;

@property (strong, nonatomic) NSArray *qsImages;
@property (strong, nonatomic) NSArray *qsLandscapeImages;
@property (assign, nonatomic) NSInteger index;
@property (strong, nonatomic) UIImageView *page;

@end
