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

/**
 * Flutter plugin for fetching files from folders with sorting capabilities.
 * This plugin provides functionality to access files from external storage
 * using Android's DocumentFile API and supports various sorting options.
 */
class FolderFilesFetchPlugin: FlutterPlugin, MethodCallHandler {
  companion object {
    // Tag for logging purposes
    private const val TAG = "FetchFilesTag"
    
    // Channel name for Flutter method communication
    private const val CHANNEL_NAME = "folder_files_fetch"

    // Method names for Flutter method calls
    private const val METHOD_FETCH_FILES = "fetchFileUriList"

    // Error codes for error handling
    private const val FETCH_ERROR = "FETCH_ERROR"

    // Sorting argument values
    private const val ASC = "asc"      // Ascending order
    private const val DESC = "desc"    // Descending order
    private const val NAME = "name"    // Sort by file name
    private const val DATE = "date"    // Sort by modification date

    // Argument names for method parameters
    private const val ARG_FOLDER_PATH = "folderPath"  // Folder path to search
    private const val ARG_SORT_TYPE = "sortType"      // Sort order (asc/desc)
    private const val ARG_SORT_BY = "sortBy"          // Sort criteria (name/date)
  }

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel: MethodChannel
  private lateinit var context: Context
  
  // Sorting parameters received from Flutter
  var sortType: String? = null  // asc or desc
  var sortBy: String? = null    // name or date

  // Custom coroutine scope for better performance and lifecycle management
  // Uses SupervisorJob to handle exceptions gracefully and IO dispatcher for file operations
  private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /**
   * Called when the plugin is attached to the Flutter engine.
   * Initializes the method channel and sets up the method call handler.
   * 
   * @param flutterPluginBinding The binding that provides access to the Flutter engine
   */
  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
    channel.setMethodCallHandler(this)
  }

  /**
   * Handles incoming method calls from Flutter.
   * Routes the method calls to appropriate handler functions.
   * 
   * @param call The method call from Flutter
   * @param result The result callback to send response back to Flutter
   */
  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
      METHOD_FETCH_FILES -> fetchFolderFiles(call, result)
      else -> result.notImplemented()
    }
  }

  /**
   * Fetches files from the specified folder path with sorting options.
   * This function runs asynchronously using coroutines to avoid blocking the main thread.
   * 
   * @param call The method call containing folder path and sorting parameters
   * @param result The result callback to return the list of file URIs
   */
  private fun fetchFolderFiles(call: MethodCall, result: MethodChannel.Result) {
    pluginScope.launch {
      try {
        // Extract parameters from the method call
        val path = call.argument<String>(ARG_FOLDER_PATH)
        sortType = call.argument<String>(ARG_SORT_TYPE)
        sortBy = call.argument<String>(ARG_SORT_BY)
        
        // Get the list of file URIs from the specified path
        val uriList = getUriList(path ?: "")
        
        // Switch to main thread to send result back to Flutter
        withContext(Dispatchers.Main) {
          result.success(uriList)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error fetching files: ${e.message}", e)
        // Switch to main thread to send error back to Flutter
        withContext(Dispatchers.Main) {
          result.error(FETCH_ERROR, e.message, null)
        }
      }
    }
  }

  /**
   * Retrieves the list of file URIs from the specified folder path.
   * This function searches through persisted URI permissions to find the matching folder
   * and returns the URIs of all files in that folder.
   * 
   * @param path The folder path to search for files
   * @return List of file URIs as strings
   */
  private suspend fun getUriList(path: String): List<String> {
    return withContext(Dispatchers.IO) {
      try {
        // Get all persisted URI permissions (folders that user has granted access to)
        val listUriPermission = context.contentResolver.persistedUriPermissions
        Log.d(
          TAG,
          "${getCurrentDateTime()} Looking for path: $path from ${listUriPermission.size} persisted URI permissions"
        )

        // Find the URI permission that matches the requested path
        val documentFiles = listUriPermission.firstOrNull {
          it.uri.path?.contains(path) == true
        }?.let { uriItem ->
          // Create DocumentFile from the matching URI
          DocumentFile.fromTreeUri(context, uriItem.uri)
        }

        // If no document files are found, return an empty list
        if (documentFiles == null) return@withContext emptyList()

        // Process and sort the files according to the specified criteria
        val sortedFiles = processDocumentFiles(documentFiles)

        Log.d(
          TAG, "${getCurrentDateTime()} Found ${sortedFiles.size} files in directory"
        )

        // Convert DocumentFile objects to URI strings
        sortedFiles.map { it.uri.toString() }
      } catch (e: Exception) {
        Log.e(TAG, "Error processing files: ${e.message}", e)
        emptyList()
      }
    }
  }

  /**
   * Processes and sorts DocumentFile objects based on the specified sorting criteria.
   * This function filters valid files and applies sorting by name or date in ascending or descending order.
   * 
   * @param documentDirectory The DocumentFile representing the directory to process
   * @return List of sorted DocumentFile objects
   */
  private suspend fun processDocumentFiles(documentDirectory: DocumentFile?): List<DocumentFile> {
    return withContext(Dispatchers.IO) {
      // Get all files in the directory
      val files = documentDirectory?.listFiles() ?: emptyArray()

      // Collect valid files with their metadata for sorting
      val validFilesWithMetadata = files.mapNotNull { file ->
        FileWithMetadata(file)
      }

      Log.d(TAG, "SortType $sortType and SortBy $sortBy")

      // Apply sorting based on the specified criteria
      val sortedFiles = when {
        (sortType ?: "") == ASC -> {
          // Sort in ascending order
          when (sortBy ?: "") {
            NAME -> validFilesWithMetadata.sortedBy { it.fileName }.map { it.documentFile }
            DATE -> validFilesWithMetadata.sortedBy { it.lastModified }.map { it.documentFile }
            else -> validFilesWithMetadata.map { it.documentFile }
          }
        }
        (sortType ?: "") == DESC -> {
          // Sort in descending order
          when (sortBy ?: "") {
            NAME -> validFilesWithMetadata.sortedByDescending { it.fileName }.map { it.documentFile }
            DATE -> validFilesWithMetadata.sortedByDescending { it.lastModified }.map { it.documentFile }
            else -> validFilesWithMetadata.map { it.documentFile }
          }
        }
        else -> {
          // No sorting applied, return files in original order
          validFilesWithMetadata.map { it.documentFile }
        }
      }

      sortedFiles
    }
  }

  /**
   * Data class to hold file metadata for sorting purposes.
   * This class wraps a DocumentFile with its name and last modified time
   * to enable efficient sorting operations.
   * 
   * @param documentFile The DocumentFile object
   * @param fileName The name of the file (defaults to documentFile.name)
   * @param lastModified The last modified timestamp (defaults to documentFile.lastModified())
   */
  private data class FileWithMetadata(
    val documentFile: DocumentFile,
    val fileName: String = documentFile.name.toString(),
    val lastModified: Long = documentFile.lastModified()
  )

  /**
   * Gets the current date and time formatted as a string.
   * This function is used for logging purposes to timestamp log entries.
   * 
   * @return Current time formatted as "HH:mm:ss"
   */
  @TargetApi(Build.VERSION_CODES.O)
  fun getCurrentDateTime(): String {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return now.format(formatter)
  }

  /**
   * Called when the plugin is detached from the Flutter engine.
   * Cleans up resources by removing the method call handler.
   * 
   * @param binding The binding that provided access to the Flutter engine
   */
  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
