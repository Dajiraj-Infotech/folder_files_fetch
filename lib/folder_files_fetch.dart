
import 'folder_files_fetch_platform_interface.dart';

class FolderFilesFetch {
  Future<String?> getPlatformVersion() {
    return FolderFilesFetchPlatform.instance.getPlatformVersion();
  }
}
