import 'folder_files_fetch_platform_interface.dart';

// Export enums to make them available to users of the plugin
export 'package:folder_files_fetch/src/utils/enums.dart';

class FolderFilesFetch {
  /// Fetches a list of file URIs from the specified folder path.
  ///
  /// This method retrieves all files within the given folder and returns their URIs
  /// as a list of strings. The files can be sorted according to the specified criteria.
  ///
  /// **Parameters:**
  /// - [folderPath] (required): The absolute path to the folder from which to fetch files.
  ///   This should be a valid directory path on the device's file system.
  /// - [sortType] (optional): The type of sorting to apply to the file list.
  ///   - `SortType.none`: No sorting applied (default)
  ///   - `SortType.ascending`: Sort in ascending order
  ///   - `SortType.descending`: Sort in descending order
  /// - [sortBy] (optional): The criteria to use for sorting the files.
  ///   - `SortBy.none`: No specific sorting criteria (default)
  ///   - `SortBy.name`: Sort by file name
  ///   - `SortBy.size`: Sort by file size
  ///   - `SortBy.date`: Sort by modification date
  ///
  /// **Returns:**
  /// A [Future<List<String>>] that completes with a list of file URIs.
  /// Each URI is a string representing the path to a file in the specified folder.
  ///
  /// **Throws:**
  /// - [FileSystemException] if the folder path is invalid or inaccessible
  /// - [PermissionException] if the app doesn't have permission to access the folder
  ///
  /// **Example Usage:**
  /// ```dart
  /// final folderFilesFetch = FolderFilesFetch();
  ///
  /// // Fetch all files without sorting
  /// List<String> files = await folderFilesFetch.fetchFileUriList(
  ///   folderPath: '/path/to/folder'
  /// );
  ///
  /// // Fetch files sorted by name in ascending order
  /// List<String> sortedFiles = await folderFilesFetch.fetchFileUriList(
  ///   folderPath: '/path/to/folder',
  ///   sortType: SortType.ascending,
  ///   sortBy: SortBy.name
  /// );
  /// ```
  Future<List<String>> fetchFileUriList({
    required String folderPath,
    SortType sortType = SortType.none,
    SortBy sortBy = SortBy.none,
  }) {
    return FolderFilesFetchPlatform.instance.fetchFileUriList(
      folderPath: folderPath,
      sortType: sortType,
      sortBy: sortBy,
    );
  }
}
