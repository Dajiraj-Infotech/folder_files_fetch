package com.dajiraj.folder_files_fetch

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Environment
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.File
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
    
    // Android version constants
    private const val ANDROID_Q = Build.VERSION_CODES.Q  // Android 10
    private const val ANDROID_9_API_LEVEL = 28  // Android 9 (Pie)
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
        val uriList = getUriListWithVersionCheck(path ?: "")
        
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
   * Retrieves the list of file URIs from the specified folder path for Android 9 and below.
   * This function uses full storage permission to access files directly from the file system.
   * 
   * @param path The folder path to search for files
   * @return List of file URIs as strings
   */
  private suspend fun getUriListLegacy(path: String): List<String> {
    return withContext(Dispatchers.IO) {
      try {
        Log.d(TAG, "${getCurrentDateTime()} Using legacy storage access for path: $path")
        
        // For Android 9 and below, use direct file access
        val folder = File(path)
        
        if (!folder.exists() || !folder.isDirectory) {
          Log.w(TAG, "Path does not exist or is not a directory: $path")
          return@withContext emptyList()
        }
        
        // Get all files in the directory
        val files = folder.listFiles()?.filter { it.isFile } ?: emptyList()
        
        Log.d(TAG, "${getCurrentDateTime()} Found ${files.size} files in directory using legacy access")
        
        // Process and sort the files according to the specified criteria
        val sortedFiles = processLegacyFiles(files)
        
        // Convert File objects to URI strings
        sortedFiles.map { it.toURI().toString() }
      } catch (e: Exception) {
        Log.e(TAG, "Error processing files with legacy access: ${e.message}", e)
        emptyList()
      }
    }
  }

  /**
   * Processes and sorts File objects based on the specified sorting criteria for Android 9 and below.
   * This function filters valid files and applies sorting by name or date in ascending or descending order.
   * 
   * @param files The list of File objects to process
   * @return List of sorted File objects
   */
  private suspend fun processLegacyFiles(files: List<File>): List<File> {
    return withContext(Dispatchers.IO) {
      // Early return if no sorting is required
      if (sortType.isNullOrEmpty() || sortBy.isNullOrEmpty()) {
        return@withContext files
      }

      Log.d(TAG, "Legacy sorting - SortType: $sortType, SortBy: $sortBy")

      val sortedFiles = when (sortBy) {
        NAME -> {
          if (sortType == ASC) {
            files.sortedBy { it.name }
          } else {
            files.sortedByDescending { it.name }
          }
        }
        DATE -> {
          if (sortType == ASC) {
            files.sortedBy { it.lastModified() }
          } else {
            files.sortedByDescending { it.lastModified() }
          }
        }
        else -> files
      }

      sortedFiles
    }
  }

  /**
   * Main function to get URI list based on Android version.
   * For Android 10 and above: uses DocumentFile API (existing logic)
   * For Android 9 and below: uses direct file access (new legacy logic)
   * 
   * @param path The folder path to search for files
   * @return List of file URIs as strings
   */
  private suspend fun getUriListWithVersionCheck(path: String): List<String> {
    return if (Build.VERSION.SDK_INT < ANDROID_Q) {
      // Android 9 and below: use improved legacy storage access
      getUriListLegacyImproved(path)
    } else {
      // Android 10 and above: use existing DocumentFile logic
      getUriList(path)
    }
  }

  /**
   * Improved function for Android 9 and below that handles path resolution better.
   * This function tries multiple path formats and provides better error handling.
   * 
   * @param path The folder path to search for files
   * @return List of file URIs as strings
   */
  private suspend fun getUriListLegacyImproved(path: String): List<String> {
    return withContext(Dispatchers.IO) {
      try {
        Log.d(TAG, "${getCurrentDateTime()} Using improved legacy storage access for path: $path")
        
        // Check storage permissions first
        if (!hasStoragePermissions()) {
          Log.w(TAG, "Storage permissions not granted for Android 9 and below")
          return@withContext emptyList()
        }
        
        // Handle different path formats for Android 9 and below
        var folder: File? = null
        
        // Try the original path first
        if (path.isNotEmpty()) {
          folder = when {
            path.startsWith("/") -> File(path)
            path.startsWith("file://") -> File(path.substring(7))
            else -> File(Environment.getExternalStorageDirectory(), path)
          }
        }
        
        // Final check if we found a valid folder
        if (folder == null || !folder.exists() || !folder.isDirectory) {
          Log.w(TAG, "No accessible folder found for path: $path")
          return@withContext emptyList()
        }
        
        // Get all files in the directory
        val allFiles = folder.listFiles()
        val files = allFiles?.filter { it.isFile } ?: emptyList()
        
        Log.d(TAG, "${getCurrentDateTime()} Found ${files.size} files in directory using improved legacy access")
        
        // Process and sort the files according to the specified criteria
        val sortedFiles = processLegacyFiles(files)
        
        // Convert File objects to URI strings
        sortedFiles.map { it.toURI().toString() }
      } catch (e: Exception) {
        Log.e(TAG, "Error processing files with improved legacy access: ${e.message}", e)
        emptyList()
      }
    }
  }

  /**
   * Checks if the app has storage permissions for Android 9 and below.
   * This is useful for debugging permission issues.
   * 
   * @return True if storage is accessible, false otherwise
   */
  private fun hasStoragePermissions(): Boolean {
    return try {
      val externalStorage = Environment.getExternalStorageDirectory()
      externalStorage.exists() && externalStorage.canRead()
    } catch (e: Exception) {
      Log.w(TAG, "Error checking storage permissions: ${e.message}")
      false
    }
  }

  /**
   * Processes and sorts DocumentFile objects based on the specified sorting criteria.
   * This function filters valid files and applies sorting by name or date in ascending or descending order.
   * Optimized to minimize system calls and maximize performance for large directories.
   * 
   * @param documentDirectory The DocumentFile representing the directory to process
   * @return List of sorted DocumentFile objects
   */
  private suspend fun processDocumentFiles(documentDirectory: DocumentFile?): List<DocumentFile> {
    return withContext(Dispatchers.IO) {
      // Get all files in the directory
      val files = documentDirectory?.listFiles() ?: emptyArray()
      
      // Filter out null files
      val validFiles = files.filterNotNull()

      Log.d(TAG, "SortType $sortType and SortBy $sortBy")

      // Early return if no sorting is required - avoid metadata fetching entirely
      if (sortType.isNullOrEmpty() || sortBy.isNullOrEmpty()) {
        return@withContext validFiles
      }

      // For sorting, we need to fetch metadata efficiently in batch
      // This approach fetches all needed metadata concurrently, then sorts based on cached values
      val sortedFiles = when (sortBy) {
        NAME -> {
          // Create pairs of (file, fileName) using concurrent fetching
          val deferredFilesWithNames = validFiles.map { file ->
            async {
              val fileName = try {
                file.name ?: ""
              } catch (e: Exception) {
                Log.w(TAG, "Error getting file name: ${e.message}")
                ""
              }
              Pair(file, fileName)
            }
          }
          
          val filesWithNames = deferredFilesWithNames.awaitAll()
          
          // Sort based on cached file names
          val sortedPairs = if (sortType == ASC) {
            filesWithNames.sortedBy { it.second }
          } else {
            filesWithNames.sortedByDescending { it.second }
          }
          
          // Extract just the files
          sortedPairs.map { it.first }
        }
        DATE -> {
          // Create pairs of (file, lastModified) using concurrent fetching
          val deferredFilesWithDates = validFiles.map { file ->
            async {
              val lastModified = try {
                file.lastModified()
              } catch (e: Exception) {
                Log.w(TAG, "Error getting last modified time: ${e.message}")
                0L
              }
              Pair(file, lastModified)
            }
          }
          
          val filesWithDates = deferredFilesWithDates.awaitAll()
          
          // Sort based on cached dates
          val sortedPairs = if (sortType == ASC) {
            filesWithDates.sortedBy { it.second }
          } else {
            filesWithDates.sortedByDescending { it.second }
          }
          
          // Extract just the files
          sortedPairs.map { it.first }
        }
        else -> validFiles
      }

      sortedFiles
    }
  }

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
