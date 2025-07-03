import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'folder_files_fetch_method_channel.dart';

abstract class FolderFilesFetchPlatform extends PlatformInterface {
  /// Constructs a FolderFilesFetchPlatform.
  FolderFilesFetchPlatform() : super(token: _token);

  static final Object _token = Object();

  static FolderFilesFetchPlatform _instance = MethodChannelFolderFilesFetch();

  /// The default instance of [FolderFilesFetchPlatform] to use.
  ///
  /// Defaults to [MethodChannelFolderFilesFetch].
  static FolderFilesFetchPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FolderFilesFetchPlatform] when
  /// they register themselves.
  static set instance(FolderFilesFetchPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
