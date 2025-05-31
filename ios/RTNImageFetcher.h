#import <React/RCTEventEmitter.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <RTNImageFetcherSpec/RTNImageFetcherSpec.h>

NS_ASSUME_NONNULL_BEGIN

// We need to declare the Objective-C class that will bridge to our Swift code.
// The name here should match what TurboModule expects, usually <ModuleName>.<ModuleClassName>
// However, for Swift, we often use a specific name for the ObjC bridging class.
@interface RTNImageFetcher : NSObject <NativeImageFetcherSpec>
@end

NS_ASSUME_NONNULL_END

#else

#import <React/RCTBridgeModule.h>

@interface RTNImageFetcher : NSObject <RCTBridgeModule>
@end

#endif

