import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'folder_files_fetch_platform_interface.dart';

/// An implementation of [FolderFilesFetchPlatform] that uses method channels.
class MethodChannelFolderFilesFetch extends FolderFilesFetchPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('folder_files_fetch');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
