package com.github.k1rakishou.chan.core.site.sites.foolfuuka

import com.github.k1rakishou.chan.core.net.JsonReaderRequest
import com.github.k1rakishou.chan.core.site.SiteActions
import com.github.k1rakishou.chan.core.site.SiteAuthentication
import com.github.k1rakishou.chan.core.site.common.CommonClientException
import com.github.k1rakishou.chan.core.site.common.CommonSite
import com.github.k1rakishou.chan.core.site.common.MultipartHttpCall
import com.github.k1rakishou.chan.core.site.http.DeleteRequest
import com.github.k1rakishou.chan.core.site.http.ReplyResponse
import com.github.k1rakishou.chan.core.site.http.login.AbstractLoginRequest
import com.github.k1rakishou.chan.core.site.sites.search.FoolFuukaSearchParams
import com.github.k1rakishou.chan.core.site.sites.search.SearchParams
import com.github.k1rakishou.chan.core.site.sites.search.SearchResult
import com.github.k1rakishou.common.ModularResult
import com.github.k1rakishou.model.data.board.ChanBoard
import com.github.k1rakishou.model.data.board.pages.BoardPages
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor
import com.github.k1rakishou.model.data.site.SiteBoards
import com.github.k1rakishou.persist_state.ReplyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.HttpUrl
import okhttp3.Request

class FoolFuukaActions(site: CommonSite) : CommonSite.CommonActions(site) {

  override suspend fun post(replyChanDescriptor: ChanDescriptor, replyMode: ReplyMode): Flow<SiteActions.PostResult> {
    return flow {
      val error = CommonClientException("Posting is not supported for site ${site.name()}")
      emit(SiteActions.PostResult.PostError(error))
    }
  }

  override fun setupPost(replyChanDescriptor: ChanDescriptor, call: MultipartHttpCall): ModularResult<Unit> {
    val error = CommonClientException("Posting is not supported for site ${site.name()}")

    return ModularResult.error(error)
  }

  override fun postAuthenticate(): SiteAuthentication {
    return SiteAuthentication.fromNone()
  }

  override fun requirePrepare(): Boolean {
    return false
  }

  override suspend fun prepare(
    call: MultipartHttpCall,
    replyChanDescriptor: ChanDescriptor,
    replyResponse: ReplyResponse
  ): ModularResult<Unit> {
    val error = CommonClientException("Posting is not supported for site ${site.name()}")

    return ModularResult.error(error)
  }

  override suspend fun delete(deleteRequest: DeleteRequest): SiteActions.DeleteResult {
    val error = CommonClientException("Post deletion is not supported for site ${site.name()}")

    return SiteActions.DeleteResult.DeleteError(error)
  }

  override suspend fun boards(): ModularResult<SiteBoards> {
    val boardsEndpoint = site.endpoints().boards()
    if (boardsEndpoint == null) {
      return ModularResult.error(CommonClientException("Site ${site.name()} does not have support for boards request"))
    }

    val request = Request.Builder()
      .url(boardsEndpoint)
      .get()
      .build()

    return FoolFuukaBoardsRequest(
      siteDescriptor = site.siteDescriptor(),
      request = request,
      proxiedOkHttpClient = site.proxiedOkHttpClient
    ).execute()
  }

  override suspend fun pages(board: ChanBoard): JsonReaderRequest.JsonReaderResponse<BoardPages>? {
    return null
  }

  override suspend fun <T : AbstractLoginRequest> login(loginRequest: T): SiteActions.LoginResult {
    return SiteActions.LoginResult.LoginError(
      "Login is not supported for site ${site.name()}"
    )
  }

  override suspend fun <T : SearchParams> search(
    searchParams: T
  ): SearchResult {
    searchParams as FoolFuukaSearchParams

    val searchUrl = requireNotNull(site.endpoints().search())
      .newBuilder()
      .addEncodedPathSegment(searchParams.boardDescriptor.boardCode)
      .addEncodedPathSegment("search")
      .tryAddSearchParam("text", searchParams.query)
      .tryAddSearchParam("subject", searchParams.subject)
      .addEncodedPathSegment("page")
      .addEncodedPathSegment(searchParams.getCurrentPage().toString())
      .build()

    val requestBuilder = Request.Builder()
      .url(searchUrl)
      .get()

    site.requestModifier().modifySearchGetRequest(site, requestBuilder)

    return FoolFuukaSearchRequest(
      searchParams,
      requestBuilder.build(),
      site.proxiedOkHttpClientLazy.get()
    ).execute()
  }

  private fun HttpUrl.Builder.tryAddSearchParam(paramName: String, paramValue: String): HttpUrl.Builder {
    if (paramValue.isEmpty()) {
      return this
    }

    return this.addEncodedPathSegment(paramName)
      .addEncodedPathSegment(paramValue)
  }

}