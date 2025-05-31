#import "RTNImageFetcher.h"
#import <React/RCTLog.h>
#import <Photos/Photos.h> // Added for PHPhotoLibrary, PHAsset etc.

// Removed Swift bridging header import: #import <rtn_image_fetcher/rtn_image_fetcher-Swift.h>

@implementation RTNImageFetcher // Removed: { ImageFetcher *_swiftModule; }

// This is the C++ specific part for TurboModules
std::shared_ptr<facebook::react::TurboModule> RTNImageFetcher_init(const facebook::react::ObjCTurboModule::InitParams &params)
{
    return std::make_shared<facebook::react::NativeImageFetcherSpecJSI>(params);
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        // Removed Swift module initialization: _swiftModule = [[ImageFetcher alloc] init];
    }
    return self;
}

// Implement the getPhotos method directly in Objective-C
- (void)getPhotos:(NSDictionary *)options resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject
{
    // Extract options
    NSNumber *limitNumber = options[@"limit"] ?: @20;
    NSInteger limit = [limitNumber integerValue];
    NSNumber *offsetNumber = options[@"offset"] ?: @0;
    NSInteger offset = [offsetNumber integerValue];
    NSString *sortBy = options[@"sortBy"] ?: @"creationDate";
    NSString *sortOrder = options[@"sortOrder"] ?: @"desc";
    // NSString *mediaTypeFilter = options[@"mediaType"]; // Example: "photo", "video", "all"

    // Permission Check
    PHAccessLevel requiredAccessLevel = PHAccessLevelReadWrite; // or .addOnly if you only add assets
    [PHPhotoLibrary requestAuthorizationForAccessLevel:requiredAccessLevel handler:^(PHAuthorizationStatus status) {
        if (status != PHAuthorizationStatusAuthorized && status != PHAuthorizationStatusLimited) {
            reject(@"PERMISSION_DENIED", @"Photo library access was denied or restricted.", nil);
            return;
        }

        PHFetchOptions *fetchOptions = [[PHFetchOptions alloc] init];

        // Sorting
        NSMutableArray *sortDescriptors = [NSMutableArray array];
        if ([sortBy isEqualToString:@"creationDate"]) {
            [sortDescriptors addObject:[NSSortDescriptor sortDescriptorWithKey:@"creationDate" ascending:[sortOrder isEqualToString:@"asc"]]];
        } else if ([sortBy isEqualToString:@"modificationDate"]) {
            [sortDescriptors addObject:[NSSortDescriptor sortDescriptorWithKey:@"modificationDate" ascending:[sortOrder isEqualToString:@"asc"]]];
        }
        // Add more sort options if needed
        fetchOptions.sortDescriptors = sortDescriptors;

        // Predicate (Filtering) - Example: by media type
        // if (mediaTypeFilter && ![mediaTypeFilter isEqualToString:@"all"]) {
        //     if ([mediaTypeFilter isEqualToString:@"photo"]) {
        //         fetchOptions.predicate = [NSPredicate predicateWithFormat:@"mediaType == %d", PHAssetMediaTypeImage];
        //     } else if ([mediaTypeFilter isEqualToString:@"video"]) {
        //         fetchOptions.predicate = [NSPredicate predicateWithFormat:@"mediaType == %d", PHAssetMediaTypeVideo];
        //     }
        // }
        
        // Fetching assets
        PHFetchResult<PHAsset *> *allAssets = [PHAsset fetchAssetsWithOptions:fetchOptions];
        NSUInteger totalAssets = allAssets.count;

        NSMutableArray *assetsArray = [NSMutableArray array];
        NSUInteger endIndex = MIN(offset + limit, totalAssets);

        if (offset >= totalAssets) {
            resolve(@{@"assets": @[], @"hasNextPage": @NO, @"nextOffset": @(offset)});
            return;
        }
        
        NSIndexSet *indexSet = [NSIndexSet indexSetWithIndexesInRange:NSMakeRange(offset, endIndex - offset)];
        [allAssets enumerateObjectsAtIndexes:indexSet options:0 usingBlock:^(PHAsset *asset, NSUInteger idx, BOOL *stop) {
            NSMutableDictionary *assetDict = [NSMutableDictionary dictionary];
            // assetDict[@"uri"] = @""; // Will be populated later if needed, or use localIdentifier
            assetDict[@"localIdentifier"] = asset.localIdentifier;
            assetDict[@"width"] = @(asset.pixelWidth);
            assetDict[@"height"] = @(asset.pixelHeight);
            assetDict[@"mediaType"] = (asset.mediaType == PHAssetMediaTypeImage) ? @"photo" : @"video";

            if (asset.creationDate) {
                assetDict[@"creationDate"] = @([asset.creationDate timeIntervalSince1970]);
            }
            if (asset.modificationDate) {
                assetDict[@"modificationDate"] = @([asset.modificationDate timeIntervalSince1970]);
            }
            if (asset.mediaType == PHAssetMediaTypeVideo) {
                assetDict[@"duration"] = @(asset.duration);
            }
            // For URI, you might need to request the actual image data or URL
            // For simplicity, we're using localIdentifier. You might need PHImageManager to get a URL.
            // [[PHImageManager defaultManager] requestImageDataAndOrientationForAsset:asset options:nil resultHandler:^(NSData * _Nullable imageData, NSString * _Nullable dataUTI, CGImagePropertyOrientation orientation, NSDictionary * _Nullable info) {
            //     NSURL *fileURL = info[@"PHImageFileURLKey"];
            //     if (fileURL) {
            //         // This part would need to be async or handled carefully if you need the URL before resolving
            //         // For now, we are keeping it simple and not fetching the direct file URL
            //     }
            // }];
            [assetsArray addObject:assetDict];
        }];

        BOOL hasNextPage = endIndex < totalAssets;
        NSUInteger nextOffset = hasNextPage ? endIndex : offset + assetsArray.count;

        resolve(@{@"assets": assetsArray, @"hasNextPage": @(hasNextPage), @"nextOffset": @(nextOffset)});
    }];
}


// Export the module to JavaScript
RCT_EXPORT_MODULE(RTNImageFetcher)

// Optional: If your Swift module needs to emit events or requires main queue setup
+ (BOOL)requiresMainQueueSetup
{
    return NO; // Set to NO as the Swift equivalent was also false
}

// If your Swift module is an RCTEventEmitter
/*
- (NSArray<NSString *> *)supportedEvents
{
    return [_swiftModule supportedEvents];
}

- (void)startObserving 
{
    [_swiftModule startObserving];
}

- (void)stopObserving 
{
    [_swiftModule stopObserving];
}
*/

// This is required for TurboModules. It tells React Native how to instantiate the module.
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeImageFetcherSpecJSI>(params);
}

@end 