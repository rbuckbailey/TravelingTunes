//
//  quickstartViewController.m
//  Traveling Tunes
//
//  Created by buck on 4/10/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "quickstartViewController.h"
#import "settingsTableViewController.h"


@interface quickstartViewController ()
@property int activeOrientation;
@property UIButton *acceptButton;



@end


@implementation quickstartViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void) orientationChanged:(NSNotification *)note{
    if ([[UIDevice currentDevice] orientation] != 5) {
        _activeOrientation = [[UIDevice currentDevice] orientation];
    }
    if (UIInterfaceOrientationIsLandscape(_activeOrientation)) {
        int width = self.view.bounds.size.width; int height = self.view.bounds.size.height;
        //the bounds frequently return wrongly in viewDidLoad... so if "landscape" is tall, invert it
        if (width<height) { int temp=width; width=height; height=temp; }
        _page.frame=CGRectMake(0,0,width,height);
        _page.image=[UIImage imageNamed:[_qsLandscapeImages objectAtIndex:_index]];
        _acceptButton.frame = CGRectMake((self.view.bounds.size.width/2)-50, self.view.bounds.size.height-40, 100, 50);
        NSLog(@"landscape: %f by %f",self.view.bounds.size.width,self.view.bounds.size.height);

    } else {
        _page.frame=CGRectMake(0,0,self.view.bounds.size.width,self.view.bounds.size.height);
        _page.image=[UIImage imageNamed:[_qsImages objectAtIndex:_index]];
        _acceptButton.frame = CGRectMake((self.view.bounds.size.width/2)-50, self.view.bounds.size.height-40, 100, 50);
        NSLog(@"portrait: %f by %f",self.view.bounds.size.width,self.view.bounds.size.height);

    }
}


- (void) viewDidAppear:(BOOL)animated
{
//    _page.frame=CGRectMake(0,0,self.view.bounds.size.width,self.view.bounds.size.height);
    if (UIInterfaceOrientationIsLandscape([[UIDevice currentDevice] orientation])) {
        _page.image=[UIImage imageNamed:[_qsLandscapeImages objectAtIndex:_index]];
        _acceptButton.frame = CGRectMake((self.view.bounds.size.width/2)-50, self.view.bounds.size.height-40, 100, 50);
        NSLog(@"appear landscape: %f by %f",self.view.bounds.size.width,self.view.bounds.size.height);

    } else {
        _page.image=[UIImage imageNamed:[_qsImages objectAtIndex:_index]];
        _acceptButton.frame = CGRectMake((self.view.bounds.size.width/2)-50, self.view.bounds.size.height-40, 100, 50);
        NSLog(@"appear portrait: %f by %f",self.view.bounds.size.width,self.view.bounds.size.height);
    }

}


- (BOOL)prefersStatusBarHidden {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ([[defaults objectForKey:@"ShowStatusBar"] isEqual:@"NO"]) return YES;
    else return NO;
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    //notifier for orientation changes
    [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
    [[NSNotificationCenter defaultCenter]
     addObserver:self selector:@selector(orientationChanged:)
     name:UIDeviceOrientationDidChangeNotification
     object:[UIDevice currentDevice]];
    
    // Do any additional setup after loading the view.
//    self.backgroundImageView.image = [UIImage imageNamed:self.imageFile];
    
    UIPageControl *pageControl = [UIPageControl appearance];
    pageControl.pageIndicatorTintColor = [UIColor lightGrayColor];
    pageControl.currentPageIndicatorTintColor = [UIColor whiteColor];
    pageControl.backgroundColor = [UIColor blackColor];
    
  //  _index=1;
    _qsImages = [[NSArray alloc] initWithObjects:@"quick start 1p.png",@"quick start 2p.png",@"quick start 3p.png",@"quick start 4p.png", nil];
    _qsLandscapeImages = [[NSArray alloc] initWithObjects:@"quick start 1.png",@"quick start 2.png",@"quick start 3.png",@"quick start 4.png", nil];
    _acceptButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];

    if (UIInterfaceOrientationIsLandscape([UIDevice currentDevice].orientation)) {
        int width = self.view.bounds.size.width; int height = self.view.bounds.size.height;
        //the bounds frequently return wrongly in viewDidLoad... so if "landscape" is tall, invert it
        if (width<height) { int temp=width; width=height; height=temp; }
        _page = [[UIImageView alloc] initWithFrame:CGRectMake(0,0,width,height-30)];
        _page.image=[UIImage imageNamed:[_qsLandscapeImages objectAtIndex:_index]];
        [self.view addSubview:_page];

        _acceptButton.frame = CGRectMake((width/2)-50, height-40, 100, 50);
        [_acceptButton setTitle:@"Done" forState:UIControlStateNormal];
        [_acceptButton addTarget:self action:@selector(buttonClicked) forControlEvents:UIControlEventTouchUpInside];
        [self.view addSubview:_acceptButton];

        NSLog(@"load landscape: %f by %f",self.view.bounds.size.width,self.view.bounds.size.height);
    } else {
        _page = [[UIImageView alloc] initWithFrame:CGRectMake(0,0,self.view.bounds.size.width,self.view.bounds.size.height)];
        _page.image=[UIImage imageNamed:[_qsImages objectAtIndex:_index]];
        [self.view addSubview:_page];

        _acceptButton.frame = CGRectMake((self.view.bounds.size.width/2)-50, self.view.bounds.size.height-40, 100, 50);
        [_acceptButton setTitle:@"Done" forState:UIControlStateNormal];
        [_acceptButton addTarget:self action:@selector(buttonClicked) forControlEvents:UIControlEventTouchUpInside];
        [self.view addSubview:_acceptButton];
        
        NSLog(@"load portrait: %f by %f",self.view.bounds.size.width,self.view.bounds.size.height);
    }


}

- (void)buttonClicked{
    [self.navigationController popViewControllerAnimated:YES];
    [self removeFromParentViewController];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
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
