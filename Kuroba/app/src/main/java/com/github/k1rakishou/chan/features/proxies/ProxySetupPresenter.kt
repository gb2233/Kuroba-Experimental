package com.github.k1rakishou.chan.features.proxies

import com.github.k1rakishou.chan.Chan
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.base.BasePresenter
import com.github.k1rakishou.chan.core.manager.ProxyStorage
import com.github.k1rakishou.chan.features.proxies.data.ProxyEntryView
import com.github.k1rakishou.chan.features.proxies.data.ProxyEntryViewSelection
import com.github.k1rakishou.chan.features.proxies.data.ProxySetupState
import com.github.k1rakishou.chan.utils.AndroidUtils.getString
import com.github.k1rakishou.chan.utils.Logger
import com.github.k1rakishou.common.errorMessageOrClassName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class ProxySetupPresenter(
  private val proxySelectionHelper: ProxySelectionHelper
) : BasePresenter<ProxySetupView>() {
  @Inject
  lateinit var proxyStorage: ProxyStorage

  @ExperimentalCoroutinesApi
  private val proxySetupState = MutableStateFlow<ProxySetupState>(ProxySetupState.Uninitialized)

  @OptIn(ExperimentalTime::class)
  override fun onCreate(view: ProxySetupView) {
    super.onCreate(view)
    Chan.inject(this)

    scope.launch {
      proxySelectionHelper.listenForSelectionChanges()
        .debounce(100.milliseconds)
        .collect { reloadProxies() }
    }

    reloadProxies()
  }

  fun listenForStateUpdates(): Flow<ProxySetupState> {
    return proxySetupState
  }

  fun toggleProxyEnableDisableState(proxyEntryView: ProxyEntryView) {
    scope.launch(Dispatchers.Default) {
      proxyStorage.enableDisableProxy(proxyEntryView)
        .safeUnwrap { error ->
          Logger.e(TAG, "Failed to enable/disable proxy", error)

          withView {
            val message = getString(
              R.string.controller_proxy_setup_failed_to_enable_disable_proxy,
              error.errorMessageOrClassName()
            )

            showMessage(message)
          }

          return@launch
        }

      reloadProxies()
    }
  }

  fun deleteProxies(selectedItems: List<ProxyStorage.ProxyKey>) {
    scope.launch(Dispatchers.Default) {
      proxyStorage.deleteProxies(selectedItems)
        .safeUnwrap { error ->
          Logger.e(TAG, "Failed to delete proxies: ${selectedItems}", error)

          withView {
            val message = getString(
              R.string.controller_proxy_setup_failed_to_delete_proxies,
              error.errorMessageOrClassName()
            )

            showMessage(message)
          }

          return@launch
        }

      reloadProxies()
    }
  }

  fun reloadProxies() {
    scope.launch(Dispatchers.Default) {
      proxyStorage.loadProxies()

      val allProxies = proxyStorage.getAllProxies()
      if (allProxies.isEmpty()) {
        proxySetupState.value = ProxySetupState.Empty
        return@launch
      }

      val proxyEntryViewList = allProxies
        .sortedBy { kurobaProxy -> kurobaProxy.order }
        .map { kurobaProxy ->
          val proxySelection = if (proxySelectionHelper.isInSelectionMode()) {
            val isSelected = proxySelectionHelper.isSelected(kurobaProxy.proxyKey)
            ProxyEntryViewSelection(isSelected)
          } else {
            null
          }

          return@map ProxyEntryView(
            address = kurobaProxy.address,
            port = kurobaProxy.port,
            enabled = kurobaProxy.enabled,
            selection = proxySelection,
            supportedSites = kurobaProxy.supportedSites.joinToString { siteDescriptor ->
              siteDescriptor.siteName
            },
            supportedActions = kurobaProxy.supportedActions.joinToString { proxyActionType ->
              proxyActionTypeToString(proxyActionType)
            },
            proxyType = proxyTypeToString(kurobaProxy.proxyType)
          )
        }

      proxySetupState.value = ProxySetupState.Data(proxyEntryViewList)
    }
  }

  private fun proxyTypeToString(proxyType: ProxyStorage.KurobaProxyType): String {
    return when (proxyType) {
      ProxyStorage.KurobaProxyType.HTTP -> "HTTP"
      ProxyStorage.KurobaProxyType.SOCKS -> "SOCKS"
    }
  }

  private fun proxyActionTypeToString(proxyActionType: ProxyStorage.ProxyActionType): String {
    return when (proxyActionType) {
      ProxyStorage.ProxyActionType.SiteRequests -> "Site requests"
      ProxyStorage.ProxyActionType.SiteMediaFull -> "Site full media loading"
      ProxyStorage.ProxyActionType.SiteMediaPreviews -> "Site media preview loading"
    }
  }

  companion object {
    private const val TAG = "ProxySetupPresenter"
  }
}