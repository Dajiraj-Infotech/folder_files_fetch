import 'package:flutter_test/flutter_test.dart';
import 'package:folder_files_fetch/folder_files_fetch.dart';
import 'package:folder_files_fetch/folder_files_fetch_platform_interface.dart';
import 'package:folder_files_fetch/folder_files_fetch_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFolderFilesFetchPlatform
    with MockPlatformInterfaceMixin
    implements FolderFilesFetchPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FolderFilesFetchPlatform initialPlatform = FolderFilesFetchPlatform.instance;

  test('$MethodChannelFolderFilesFetch is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFolderFilesFetch>());
  });

  test('getPlatformVersion', () async {
    FolderFilesFetch folderFilesFetchPlugin = FolderFilesFetch();
    MockFolderFilesFetchPlatform fakePlatform = MockFolderFilesFetchPlatform();
    FolderFilesFetchPlatform.instance = fakePlatform;

    expect(await folderFilesFetchPlugin.getPlatformVersion(), '42');
  });
}
