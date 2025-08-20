# Changelog

All notable changes to this project will be documented in this file.

## 0.0.4

- **Scoped Storage Migration**: Removed legacy file access methods in favor of Android 10+ scoped storage implementation
- **Performance Improvements**: Streamlined codebase by removing unused legacy methods

## 0.0.3

- **Legacy Android Support**: Added support for Android 9 and below with improved file access compatibility
- **Enhanced Path Resolution**: Implemented improved path resolution for better file retrieval across Android versions

## 0.0.2

- **File Sorting**: Improved sorting logic based on name and date with enhanced performance.

## 0.0.1

### Initial Release

**Features:**
- **File Fetching**: Retrieve all files from a specified folder path on Android devices
- **Advanced Sorting**: Sort files by name or modification date with ascending/descending options
- **Android Integration**: Native Android implementation using DocumentFile API
- **Permission Handling**: Works with Android's persisted URI permissions for folder access
- **Asynchronous Operations**: Non-blocking file operations using Kotlin coroutines
- **Comprehensive Error Handling**: Proper exception handling for file system and permission errors