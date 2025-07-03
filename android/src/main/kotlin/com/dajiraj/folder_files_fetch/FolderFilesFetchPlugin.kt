package com.dajiraj.folder_files_fetch

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** FolderFilesFetchPlugin */
class FolderFilesFetchPlugin: FlutterPlugin, MethodCallHandler {
  companion object {
    private const val TAG = "FetchFilesTag"
    private const val CHANNEL_NAME = "folder_files_fetch"

    // Method names
    private const val METHOD_FETCH_FILES = "fetchFileUriList"

    // Error codes
    private const val FETCH_ERROR = "FETCH_ERROR"

    // Sorting argument values
    private const val ASC = "asc"
    private const val DESC = "desc"
    private const val NAME = "name"
    private const val DATE = "date"

    // Argument names
    private const val ARG_FOLDER_PATH = "folderPath"
    private const val ARG_SORT_TYPE = "sortType"
    private const val ARG_SORT_BY = "sortBy"
  }

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel: MethodChannel
  private lateinit var context: Context
  var sortType: String? = null
  var sortBy: String? = null

  // Custom coroutine scope for better performance and lifecycle management
  private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
      METHOD_FETCH_FILES -> fetchFolderFiles(call, result)
      else -> result.notImplemented()
    }
  }

  private fun fetchFolderFiles(call: MethodCall, result: MethodChannel.Result) {
    pluginScope.launch {
      try {
        val path = call.argument<String>(ARG_FOLDER_PATH)
        sortType = call.argument<String>(ARG_SORT_TYPE)
        sortBy = call.argument<String>(ARG_SORT_BY)
        val uriList = getUriList(path ?: "")
        withContext(Dispatchers.Main) {
          result.success(uriList)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error fetching files: ${e.message}", e)
        withContext(Dispatchers.Main) {
          result.error(FETCH_ERROR, e.message, null)
        }
      }
    }
  }

  private suspend fun getUriList(path: String): List<String> {
    return withContext(Dispatchers.IO) {
      try {
        val listUriPermission = context.contentResolver.persistedUriPermissions
        Log.d(
          TAG,
          "${getCurrentDateTime()} Looking for path: $path from ${listUriPermission.size} persisted URI permissions"
        )

        val documentFiles = listUriPermission.firstOrNull {
          it.uri.path?.contains(path) == true
        }?.let { uriItem ->
          DocumentFile.fromTreeUri(context, uriItem.uri)
        }

        // If no document files are found, return an empty list
        if (documentFiles == null) return@withContext emptyList()

        // Process files in a single operation
        val sortedFiles = processDocumentFiles(documentFiles)

        Log.d(
          TAG, "${getCurrentDateTime()} Found ${sortedFiles.size} files in directory"
        )

        sortedFiles.map { it.uri.toString() }
      } catch (e: Exception) {
        Log.e(TAG, "Error processing files: ${e.message}", e)
        emptyList()
      }
    }
  }

  private suspend fun processDocumentFiles(documentDirectory: DocumentFile?): List<DocumentFile> {
    return withContext(Dispatchers.IO) {
      val files = documentDirectory?.listFiles() ?: emptyArray()

      // Collect valid files with their metadata
      val validFilesWithMetadata = files.mapNotNull { file ->
        FileWithMetadata(file)
      }

      Log.d(TAG, "SortType $sortType and SortBy $sortBy")

      val sortedFiles = when {
        (sortType ?: "") == ASC -> {
          when (sortBy ?: "") {
            NAME -> validFilesWithMetadata.sortedBy { it.fileName }.map { it.documentFile }
            DATE -> validFilesWithMetadata.sortedBy { it.lastModified }.map { it.documentFile }
            else -> validFilesWithMetadata.map { it.documentFile }
          }
        }
        (sortType ?: "") == DESC -> {
          when (sortBy ?: "") {
            NAME -> validFilesWithMetadata.sortedByDescending { it.fileName }.map { it.documentFile }
            DATE -> validFilesWithMetadata.sortedByDescending { it.lastModified }.map { it.documentFile }
            else -> validFilesWithMetadata.map { it.documentFile }
          }
        }
        else -> validFilesWithMetadata.map { it.documentFile }
      }

      sortedFiles
    }
  }

  private data class FileWithMetadata(
    val documentFile: DocumentFile,
    val fileName: String = documentFile.name.toString(),
    val lastModified: Long = documentFile.lastModified()
  )

  @TargetApi(Build.VERSION_CODES.O)
  fun getCurrentDateTime(): String {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return now.format(formatter)
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
