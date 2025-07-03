# Folder Files Fetch

A Flutter plugin for fetching files from folders with advanced sorting capabilities. This plugin provides a simple and efficient way to access files from external storage on Android devices using the DocumentFile API.

## Features

- **File Fetching**: Retrieve all files from a specified folder path
- **Advanced Sorting**: Sort files by name or modification date
- **Sorting Options**: Support for ascending and descending order
- **Android Integration**: Native Android implementation using DocumentFile API
- **Permission Handling**: Works with Android's persisted URI permissions
- **Asynchronous Operations**: Non-blocking file operations using Kotlin coroutines
- **Error Handling**: Comprehensive error handling and logging

## Platform Support

- ✅ **Android**: Full support with native implementation

## Getting Started

### Installation

Add `folder_files_fetch` to your `pubspec.yaml`:

```yaml
dependencies:
  folder_files_fetch: ^0.0.1
```

### Android Setup

#### Folder Permission

This plugin works with Android's DocumentFile API and requires folder permissions to be granted by the user. You'll need to use a separate plugin like [android_folder_permission](https://pub.dev/packages/android_folder_permission) to request and manage folder access permissions.

## Usage

### Basic Usage

```dart
import 'package:folder_files_fetch/folder_files_fetch.dart';

// Create an instance of the plugin
final folderFilesFetch = FolderFilesFetch();

// Fetch all files from a folder (no sorting)
List<String> files = await folderFilesFetch.fetchFileUriList(
  folderPath: '/path/to/your/folder'
);
```

### Advanced Usage with Sorting

```dart
// Fetch files sorted by name in ascending order
List<String> sortedFiles = await folderFilesFetch.fetchFileUriList(
  folderPath: '/path/to/your/folder',
  sortType: SortType.asc,
  sortBy: SortBy.name,
);

// Fetch files sorted by modification date in descending order
List<String> dateSortedFiles = await folderFilesFetch.fetchFileUriList(
  folderPath: '/path/to/your/folder',
  sortType: SortType.desc,
  sortBy: SortBy.date,
);
```

## API Reference

### Classes

#### FolderFilesFetch

The main class for interacting with the plugin.

**Methods:**

- `fetchFileUriList({required String folderPath, SortType sortType = SortType.none, SortBy sortBy = SortBy.none})`

  Fetches a list of file URIs from the specified folder path.
  
  **Parameters:**
  - `folderPath` (required): The absolute path to the folder
  - `sortType` (optional): The type of sorting to apply
  - `sortBy` (optional): The criteria to use for sorting
  
  **Returns:** `Future<List<String>>` - List of file URIs

### Enums

#### SortType

Defines the sorting order:

- `SortType.none`: No sorting applied (default)
- `SortType.asc`: Sort in ascending order
- `SortType.desc`: Sort in descending order

#### SortBy

Defines the sorting criteria:

- `SortBy.none`: No specific sorting criteria (default)
- `SortBy.name`: Sort by file name
- `SortBy.date`: Sort by modification date

## Error Handling

The plugin provides comprehensive error handling:

- **FileSystemException**: Thrown when the folder path is invalid or inaccessible
- **PermissionException**: Thrown when the app doesn't have permission to access the folder
- **PlatformException**: Thrown for other platform-specific errors

```dart
try {
  List<String> files = await folderFilesFetch.fetchFileUriList(
    folderPath: '/path/to/folder'
  );
} on FileSystemException catch (e) {
  print('File system error: ${e.message}');
} on PermissionException catch (e) {
  print('Permission error: ${e.message}');
} catch (e) {
  print('Unexpected error: $e');
}
```

## Technical Details

### Android Implementation

The Android implementation uses:

- **DocumentFile API**: For accessing files from external storage
- **Persisted URI Permissions**: To maintain access to folders across app sessions
- **Kotlin Coroutines**: For asynchronous file operations
- **Method Channel**: For communication between Flutter and native Android code

### Architecture

The plugin follows Flutter's plugin architecture:

1. **Platform Interface**: Defines the contract for platform implementations
2. **Method Channel**: Handles communication between Flutter and native code
3. **Platform Implementation**: Native Android code using Kotlin

### Performance Considerations

- File operations are performed on background threads using Kotlin coroutines
- Sorting is done efficiently using Kotlin's built-in sorting functions
- Memory usage is optimized by processing files in batches

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.