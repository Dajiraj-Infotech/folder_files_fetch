import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'folder_files_fetch_platform_interface.dart';

/// An implementation of [FolderFilesFetchPlatform] that uses method channels.
///
/// This class provides a concrete implementation of the platform interface
/// that communicates with native Android code through Flutter's method channel
/// system. It handles the conversion between Dart and native platform data types.
class MethodChannelFolderFilesFetch extends FolderFilesFetchPlatform {
  /// The method channel used to interact with the native platform.
  ///
  /// This channel establishes a communication bridge between Flutter (Dart) code
  /// and the native Android implementation. The channel name 'folder_files_fetch'
  /// must match the channel name used in the native Android code.
  @visibleForTesting
  final methodChannel = const MethodChannel('folder_files_fetch');

  /// Fetches a list of file URIs from a specified folder with sorting options.
  ///
  /// This method communicates with the native Android platform to retrieve
  /// all files from the given folder path. The files can be sorted according
  /// to the specified criteria before being returned.
  ///
  /// Parameters:
  /// - [folderPath]: The absolute path to the folder from which to fetch files.
  ///   This should be a valid directory path accessible by the app.
  /// - [sortType]: The type of sorting to apply (ascending or descending).
  ///   This determines the order in which files are returned.
  /// - [sortBy]: The criteria to use for sorting (name, date, size, etc.).
  ///   This determines which file property is used for comparison.
  ///
  /// Returns:
  /// - A [Future<List<String>>] containing the URIs of all files in the folder.
  ///   Each URI is a string that can be used to access the file.
  ///   Returns an empty list if no files are found or if an error occurs.
  ///
  /// Throws:
  /// - [PlatformException] if the native method call fails or if the folder
  ///   path is invalid or inaccessible.
  /// - [MissingPluginException] if the native platform implementation is not
  ///   available.
  @override
  Future<List<String>> fetchFileUriList({
    required String folderPath,
    required SortType sortType,
    required SortBy sortBy,
  }) async {
    // Invoke the native method to fetch files from the specified folder
    // The method name 'fetchFileUriList' must match the method name
    // implemented in the native Android code
    final foldersFiles = await methodChannel.invokeMethod('fetchFileUriList', {
      'folderPath': folderPath, // Pass the folder path to native code
      'sortType': sortType.name, // Pass the sort type as a string
      'sortBy': sortBy.name, // Pass the sort criteria as a string
    });

    // Handle null response from native code
    // This can happen if the native method returns null or if there's an error
    if (foldersFiles == null) return [];

    // Convert the response from native code to a proper List<String>
    // The native code might return List<Object?> which needs type conversion
    if (foldersFiles is List) {
      return foldersFiles
          .where((item) => item != null) // Filter out null items
          .map((item) => item.toString()) // Convert each item to string
          .where((item) => item.isNotEmpty) // Filter out empty strings
          .toList(); // Convert to List<String>
    }

    // Return empty list if the response is not a List type
    // This handles unexpected response formats from native code
    return [];
  }
}
