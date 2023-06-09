//
//  ttunesAppDelegate.m
//  Traveling Tunes
//
//  Created by buck on 1/31/14.
//  Copyright (c) 2014 Have Geek, Will Travel LLC. All rights reserved.
//

#import "ttunesAppDelegate.h"
//#import "gestureAssignmentController.h"
//#import "ttunesViewController.h"

@implementation ttunesAppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    return YES;
}
							
- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ([[defaults objectForKey:@"PauseOnExit"] isEqual:@"YES"]) [self.ttunes performPlayerAction:@"Pause":@"Background"];
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later. 
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    if ([[defaults objectForKey:@"PlayOnLaunch"] isEqual:@"YES"]) [self.ttunes performPlayerAction:@"Play":@"Startup"];
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
  sourceApplication:(NSString *)sourceApplication
         annotation:(id)annotation {
    if ([MKDirectionsRequest isDirectionsRequestURL:url]) {
        MKDirectionsRequest* directionsInfo = [[MKDirectionsRequest alloc] initWithContentsOfURL:url];
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        MKPlacemark *destination = directionsInfo.destination.placemark;
        NSString *destinationAddress = [NSString stringWithFormat:@"%@ %@",[destination.addressDictionary objectForKey:@"Street"],[destination.addressDictionary objectForKey:@"City"]];
        [defaults setObject:destinationAddress forKey:@"destinationAddress"];
        [defaults setObject:destinationAddress forKey:@"lastDestination"]; // remember address in "enter address" field
        [defaults synchronize];
        [self.ttunes setupDestinationAddress];
        return YES;
    }
    else {
        // Handle other URL types...
    }
    return NO;
}

@end

