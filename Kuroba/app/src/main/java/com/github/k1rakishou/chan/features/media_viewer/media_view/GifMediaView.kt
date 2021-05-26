package com.github.k1rakishou.chan.features.media_viewer.media_view

import android.annotation.SuppressLint
import android.content.Context
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.features.media_viewer.ViewableMedia
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import pl.droidsonroids.gif.GifImageView

@SuppressLint("ViewConstructor")
class GifMediaView(
  context: Context,
  private val mediaViewContract: MediaViewContract,
  private val onThumbnailFullyLoaded: () -> Unit,
  override val viewableMedia: ViewableMedia.Gif,
  override val pagerPosition: Int,
  override val totalPageItemsCount: Int
) : MediaView<ViewableMedia.Gif>(context, null) {
  private val thumbnailMediaView: ThumbnailMediaView
  private val actualGifView: GifImageView

  override val hasContent: Boolean
    get() = false // TODO(KurobaEx):

  init {
    AppModuleAndroidUtils.extractActivityComponent(context)
      .inject(this)

    inflate(context, R.layout.media_view_gif, this)

    thumbnailMediaView = findViewById(R.id.thumbnail_media_view)
    actualGifView = findViewById(R.id.actual_gif_view)
  }

  override fun preload() {
    val previewLocation = viewableMedia.previewLocation
    if (previewLocation == null) {
      return
    }

    thumbnailMediaView.bind(
      ThumbnailMediaView.ThumbnailMediaViewParameters(
        isOriginalMediaPlayable = true,
        thumbnailLocation = previewLocation
      ),
      onThumbnailFullyLoaded = onThumbnailFullyLoaded
    )
  }

  override fun bind() {

  }

  override fun show() {

  }

  override fun hide() {

  }

  override fun unbind() {
    thumbnailMediaView.unbind()
  }

  companion object {
    private const val TAG = "GifMediaView"
  }
}