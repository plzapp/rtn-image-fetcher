import type {TurboModule} from 'react-native/Libraries/TurboModule/RCTExport';
import {TurboModuleRegistry} from 'react-native';

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
  // Add other relevant fields like location, album, etc.
}

// Define the options for fetching photos
export interface FetchOptions {
  limit: number;
  offset?: number;
  sortBy?: 'creationDate' | 'modificationDate' | 'default'; // Add more as needed
  sortOrder?: 'asc' | 'desc';
  // Add filter options: mediaType, specific albums, date ranges, etc.
  // e.g. mediaType?: 'photo' | 'video' | 'all';
  // e.g. afterDate?: number; // Unix timestamp
  // e.g. beforeDate?: number; // Unix timestamp
  // e.g. includeGooglePhotos?: boolean; // Android specific
}

// Define the result of fetching photos
export interface FetchResult {
  assets: PhotoAsset[];
  hasNextPage: boolean;
  nextOffset?: number; // Or a cursor string
}

export interface Spec extends TurboModule {
  // Method to get a paginated list of photos
  getPhotos(options: FetchOptions): Promise<FetchResult>;

  // Example of another method, perhaps to request permissions
  // requestPermissions(): Promise<boolean>;

  // Add other methods your native module will expose
  // e.g., getAlbums, getPhotoById, etc.
}

export default TurboModuleRegistry.get<Spec>(
  'RTNImageFetcher',
) as Spec | null; 