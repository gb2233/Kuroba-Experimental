package com.github.k1rakishou.chan.features.settings.screens.delegate

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.github.k1rakishou.chan.BuildConfig
import com.github.k1rakishou.chan.activity.StartActivity
import com.github.k1rakishou.chan.core.repository.ImportExportRepository
import com.github.k1rakishou.chan.ui.controller.LoadingViewController
import com.github.k1rakishou.chan.ui.controller.navigation.NavigationController
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.showToast
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.fsaf.FileChooser
import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.fsaf.callback.FileChooserCallback
import com.github.k1rakishou.fsaf.callback.FileCreateCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.ISODateTimeFormat

class ImportExportSettingsDelegate(
  private val context: Context,
  private val coroutineScope: CoroutineScope,
  private val navigationController: NavigationController,
  private val fileChooser: FileChooser,
  private val fileManager: FileManager,
  private val importExportRepository: ImportExportRepository
) {
  private val loadingViewController = LoadingViewController(context, true)


  fun onExportClicked() {
    val dateString = BACKUP_DATE_FORMAT.print(DateTime.now())
    val exportFileName = "KurobaEx_v${BuildConfig.VERSION_CODE}_($dateString)_backup.zip"

    /**
     * Creates a new file with the default name (that can be changed in the file chooser) with the
     * settings. Cannot be used for overwriting an old settings file (when trying to do so a new file
     * with appended "(1)" at the end will appear, e.g. "test (1).txt")
     */
    fileChooser.openCreateFileDialog(
      exportFileName,
      object : FileCreateCallback() {
        override fun onResult(uri: Uri) {
          onExportFileChosen(uri)
        }

        override fun onCancel(reason: String) {
          showToast(context, reason, Toast.LENGTH_LONG)
        }
      })
  }

  fun onImportClicked() {
    fileChooser.openChooseFileDialog(object : FileChooserCallback() {
      override fun onResult(uri: Uri) {
        onImportFileChosen(uri)
      }

      override fun onCancel(reason: String) {
        showToast(context, reason, Toast.LENGTH_LONG)
      }
    })
  }

  fun onImportFromKurobaClicked() {
    fileChooser.openChooseFileDialog(object : FileChooserCallback() {
      override fun onResult(uri: Uri) {
        onImportFromKurobaFileChosen(uri)
      }

      override fun onCancel(reason: String) {
        showToast(context, reason, Toast.LENGTH_LONG)
      }
    })
  }

  private fun onExportFileChosen(uri: Uri) {
    // We use SAF here by default because settings importing/exporting does not depend on the
    // Kuroba default directory location. There is just no need to use old java files.
    val externalFile = fileManager.fromUri(uri)
    if (externalFile == null) {
      val message = "onFileChosen() fileManager.fromUri() returned null, uri = $uri"
      Logger.d(TAG, message)
      showToast(context, message, Toast.LENGTH_LONG)
      return
    }

    coroutineScope.launch {
      navigationController.presentController(loadingViewController)

      val result = withContext(Dispatchers.Default) {
        importExportRepository.exportTo(externalFile)
      }

      loadingViewController.stopPresenting()

      when (result) {
        is ModularResult.Error -> {
          Logger.e(TAG, "Export error", result.error)
          showToast(context, "Export error: ${result.error}")
        }
        is ModularResult.Value -> {
          showToast(context, "Export success!")
        }
      }
    }
  }

  private fun onImportFileChosen(uri: Uri) {
    val externalFile = fileManager.fromUri(uri)
    if (externalFile == null) {
      val message = "onImportClicked() fileManager.fromUri() returned null, uri = $uri"
      Logger.d(TAG, message)
      showToast(context, message, Toast.LENGTH_LONG)
      return
    }

    coroutineScope.launch {
      navigationController.presentController(loadingViewController)

      val result = withContext(Dispatchers.Default) {
        importExportRepository.importFrom(externalFile)
      }

      loadingViewController.stopPresenting()

      when (result) {
        is ModularResult.Error -> {
          Logger.e(TAG, "Import error", result.error)
          showToast(context, "Import error: ${result.error}")
        }
        is ModularResult.Value -> {
          (context as StartActivity).restartApp()
        }
      }
    }
  }

  private fun onImportFromKurobaFileChosen(uri: Uri) {
    val externalFile = fileManager.fromUri(uri)
    if (externalFile == null) {
      val message = "onImportFromKurobaFileChosen() fileManager.fromUri() returned null, uri = $uri"
      Logger.d(TAG, message)
      showToast(context, message, Toast.LENGTH_LONG)
      return
    }

    coroutineScope.launch {
      navigationController.presentController(loadingViewController)

      val result = withContext(Dispatchers.Default) {
        importExportRepository.importFromKuroba(externalFile)
      }

      loadingViewController.stopPresenting()

      when (result) {
        is ModularResult.Error -> {
          Logger.e(TAG, "Import from Kuroba error", result.error)
          showToast(context, "Import from Kuroba error: ${result.error}")
        }
        is ModularResult.Value -> {
          (context as StartActivity).restartApp()
        }
      }
    }
  }

  companion object {
    private const val TAG = "ImportExportSettingsDelegate"

    private val BACKUP_DATE_FORMAT = DateTimeFormatterBuilder()
      .append(ISODateTimeFormat.date())
      .toFormatter()
  }
}