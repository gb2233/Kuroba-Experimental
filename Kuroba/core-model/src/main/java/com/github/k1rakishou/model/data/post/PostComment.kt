package com.github.k1rakishou.model.data.post

import com.github.k1rakishou.common.MurmurHashUtils
import com.github.k1rakishou.core_spannable.PostLinkable

// Thread safe
class PostComment(
  @get:Synchronized
  @set:Synchronized
  // The original comment without any custom link spannables.
  private var originalComment: CharSequence,
  @get:Synchronized
  val originalUnparsedComment: String?,
  @get:Synchronized
  val linkables: List<PostLinkable>
) {

  @get:Synchronized
  @set:Synchronized
  private var _originalCommentHash = MurmurHashUtils.murmurhash3_x64_128(originalComment)

  @get:Synchronized
  @set:Synchronized
  // A comment version that may contain manually added spans (like link spans with link
  // video title/video duration etc)
  private var _updatedComment: CharSequence? = null

  @get:Synchronized
  @set:Synchronized
  private var _updatedCommentHash: MurmurHashUtils.Murmur3Hash? = null

  @get:Synchronized
  val originalCommentHash: MurmurHashUtils.Murmur3Hash
    get() = _originalCommentHash

  @get:Synchronized
  val updatedCommentHash: MurmurHashUtils.Murmur3Hash?
    get() = _updatedCommentHash

  @Synchronized
  fun updateComment(newComment: CharSequence) {
    this._updatedComment = newComment
    this._updatedCommentHash = MurmurHashUtils.murmurhash3_x64_128(newComment)
  }

  @Synchronized
  fun comment(): CharSequence {
    if (_updatedComment == null) {
      return originalComment
    }

    return _updatedComment!!
  }

  @Synchronized
  fun originalComment(): CharSequence {
    return originalComment
  }

  @Synchronized
  fun getAllLinkables(): List<PostLinkable> {
    return linkables.toList()
  }

  @Synchronized
  fun hasComment() = originalComment.isNotEmpty()

  @Synchronized
  fun containsPostLinkable(postLinkable: PostLinkable): Boolean {
    return linkables.contains(postLinkable)
  }

  override fun toString(): String {
    return "PostComment(originalComment='${originalComment.take(64)}\', " +
      "comment='${_updatedComment?.take(64)}', linkablesCount=${linkables.size})"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PostComment
    if (originalComment != other.originalComment) return false

    return true
  }

  override fun hashCode(): Int {
    return originalComment.hashCode()
  }

}