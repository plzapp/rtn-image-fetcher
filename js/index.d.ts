// Type definitions for rtn-image-fetcher
// Project: https://github.com/your-username/rtn-image-fetcher (Replace with your actual project URL)
// Definitions by: Your Name <your-email@example.com> (Replace with your name and email)
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

declare module 'rtn-image-fetcher' {
  // Define the interface for the photo asset
  export interface PhotoAsset {
    uri: string;
    localIdentifier?: string; // iOS local identifier
    id?: string; // Android media ID
    filename?: string;
    width: number;
    height: number;
    creationDate?: number; // Unix timestamp
    modificationDate?: number; // Unix timestamp
    mediaType: 'photo' | 'video';
    duration?: number; // For video
  }

  // Define the options for fetching photos
  export interface FetchOptions {
    limit: number;
    offset?: number;
    sortBy?: 'creationDate' | 'modificationDate' | 'default';
    sortOrder?: 'asc' | 'desc';
  }

  // Define the result of fetching photos
  export interface FetchResult {
    assets: PhotoAsset[];
    hasNextPage: boolean;
    nextOffset?: number;
  }

  // Define the interface for the native module
  export interface Spec {
    getPhotos(options: FetchOptions): Promise<FetchResult>;
  }

  const RTNImageFetcher: Spec | null;
  export default RTNImageFetcher;
} 