import 'package:android_folder_permission/android_folder_permission.dart';
import 'package:flutter/material.dart';
import 'dart:async';
import 'package:folder_files_fetch/folder_files_fetch.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _hasFolderPermission = false;
  bool _isLoading = false;
  List<String> _foldersFiles = [];

  final _androidFolderPermissionPlugin = AndroidFolderPermission();
  final _fetchFoldersFilesPlugin = FolderFilesFetch();

  final _folderPath = 'Android/media/com.whatsapp/WhatsApp/Media/.Statuses';

  @override
  void initState() {
    super.initState();
    fetchFileUriList();
  }

  // Fetch the files from the folder
  Future<void> fetchFileUriList() async {
    // Check if the folder has permission
    await checkFolderPermission();
    if (!_hasFolderPermission) return;

    setState(() => _isLoading = true);

    // Fetch the files
    try {
      _foldersFiles = await _fetchFoldersFilesPlugin.fetchFileUriList(
        folderPath: _folderPath,
        sortType: SortType.asc,
        sortBy: SortBy.name,
      );
    } on PlatformException {
      _foldersFiles = [];
    } finally {
      setState(() {
        _isLoading = false;
        _foldersFiles = _foldersFiles;
      });
    }
  }

  // Check if the folder has permission
  Future<void> checkFolderPermission() async {
    try {
      _hasFolderPermission = await _androidFolderPermissionPlugin
          .checkFolderPermission(path: _folderPath);
    } on PlatformException {
      _hasFolderPermission = false;
    }
    if (!mounted) return;
    setState(() => _hasFolderPermission = _hasFolderPermission);
  }

  // Request the folder permission
  Future<void> requestFolderPermission() async {
    try {
      final result = await _androidFolderPermissionPlugin
          .requestFolderPermission(path: _folderPath);
      debugPrint('result: $result');
      if (result.isNotEmpty) await fetchFileUriList();
    } on PlatformException catch (e) {
      debugPrint('error: ${e.message}');
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          forceMaterialTransparency: true,
          title: const Text('Fetch Files'),
        ),
        body: _buildBody(),
      ),
    );
  }

  Widget _buildBody() {
    if (!_hasFolderPermission) return _buildNoPermission();
    if (_isLoading) return _buildLoading();
    return _buildListView(context);
  }

  Widget _buildNoPermission() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              'No permission',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 16.0),
            Text(
              'Path: $_folderPath',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            const SizedBox(height: 16.0),
            ElevatedButton(
              onPressed: () => requestFolderPermission(),
              child: const Text('Request permission'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLoading() {
    return const Center(child: CircularProgressIndicator());
  }

  Widget _widgetEmpty() {
    return Center(
      child: Text(
        'No files found',
        style: Theme.of(context).textTheme.titleLarge,
      ),
    );
  }

  Widget _buildListView(BuildContext context) {
    if (_foldersFiles.isEmpty) return _widgetEmpty();

    return ListView.builder(
      padding: const EdgeInsets.all(16.0),
      itemCount: _foldersFiles.length,
      itemBuilder: (context, index) {
        return _buildListItem(context, index);
      },
    );
  }

  Widget _buildListItem(BuildContext context, int index) {
    final fileUri = _foldersFiles[index];
    final decodedUri = Uri.decodeFull(fileUri);
    final fileName = decodedUri.split('/').last;
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: const Icon(Icons.insert_drive_file_outlined, size: 40),
      title: Text(fileName),
      subtitle: Text(decodedUri, maxLines: 2, overflow: TextOverflow.ellipsis),
    );
  }
}
