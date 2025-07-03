import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'folder_files_fetch_platform_interface.dart';

/// An implementation of [FolderFilesFetchPlatform] that uses method channels.
class MethodChannelFolderFilesFetch extends FolderFilesFetchPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('folder_files_fetch');

  @override
  Future<List<String>> fetchFileUriList({
    required String folderPath,
    required SortType sortType,
    required SortBy sortBy,
  }) async {
    final foldersFiles = await methodChannel.invokeMethod('fetchFileUriList', {
      'folderPath': folderPath,
      'sortType': sortType.name,
      'sortBy': sortBy.name,
    });

    if (foldersFiles == null) return [];

    // Convert List<Object?> to List<String>
    if (foldersFiles is List) {
      return foldersFiles
          .where((item) => item != null)
          .map((item) => item.toString())
          .where((item) => item.isNotEmpty)
          .toList();
    }
    return [];
  }
}
