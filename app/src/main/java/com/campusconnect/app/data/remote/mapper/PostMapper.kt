package com.campusconnect.app.data.remote.mapper

import com.campusconnect.app.data.model.Post
import com.campusconnect.app.data.model.PostType
import com.campusconnect.app.data.model.ReactionType
import com.campusconnect.app.data.model.User
import com.campusconnect.app.data.remote.dto.PostDto

private fun String.toPostType(): PostType = when (this) {
    "image" -> PostType.IMAGE
    "video" -> PostType.VIDEO
    "poll"  -> PostType.POLL
    else    -> PostType.TEXT
}

/**
 * Backend now returns a real per-emoji reaction breakdown and the
 * requesting user's own reaction (see PostsService.enrichWithReactions) —
 * this used to fold everything into a single LIKE bucket and always show
 * `userReaction = null` as a stopgap; that workaround is gone now that the
 * backend gap is closed.
 */
fun PostDto.toDomain(fallbackAuthor: User? = null): Post = Post(
    id            = id,
    author        = user?.toDomain() ?: fallbackAuthor ?: User(
        id = userId ?: "unknown", username = "unknown", firstName = "Unknown", lastName = "",
    ),
    content       = content,
    mediaUrls     = mediaUrls,
    postType      = postType.toPostType(),
    reactions     = reactionsBreakdown.mapNotNull { (key, count) ->
        key.toReactionType()?.let { it to count }
    }.toMap(),
    commentsCount = commentsCount,
    sharesCount   = sharesCount,
    viewsCount    = viewsCount,
    communityName = null, // community relation not currently loaded by the feed endpoint
    userReaction  = myReaction.toReactionType(),
    isBookmarked  = false, // no bookmarks endpoint wired yet
    createdAt     = createdAt.toEpochMillisOrNow(),
)
