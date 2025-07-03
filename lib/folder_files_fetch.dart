import 'folder_files_fetch_platform_interface.dart';

// Export enums to make them available to users of the plugin
export 'package:folder_files_fetch/src/utils/enums.dart';

class FolderFilesFetch {
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
