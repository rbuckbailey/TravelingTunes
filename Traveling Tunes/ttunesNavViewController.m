//
//  ttunesNavViewController.m
//  Traveling Tunes
//
//  Created by buck on 3/29/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "ttunesNavViewController.h"

@interface ttunesNavViewController ()

@end

@implementation ttunesNavViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(NSUInteger)supportedInterfaceOrientations
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSInteger orientations = 0;
    if ([[defaults objectForKey:@"RotationPortrait"] isEqual:@"YES"]) orientations=orientations|UIInterfaceOrientationMaskPortrait;
    if ([[defaults objectForKey:@"RotationClockwise"] isEqual:@"YES"]) orientations=orientations|UIInterfaceOrientationMaskLandscapeLeft;
    if ([[defaults objectForKey:@"RotationAntiClockwise"] isEqual:@"YES"]) orientations=orientations|UIInterfaceOrientationMaskLandscapeRight;
    if ([[defaults objectForKey:@"RotationInverted"] isEqual:@"YES"]) orientations=orientations|UIInterfaceOrientationMaskPortraitUpsideDown;
    return orientations;
}

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
