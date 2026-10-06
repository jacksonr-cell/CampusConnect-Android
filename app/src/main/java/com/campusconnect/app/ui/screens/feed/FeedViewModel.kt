package com.campusconnect.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.app.data.model.*
import com.campusconnect.app.data.repository.ApiResult
import com.campusconnect.app.data.repository.PostsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FeedViewModel(private val postsRepository: PostsRepository) : ViewModel() {

    private val _feedState = MutableStateFlow<FeedState>(FeedState.Loading)
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    /**
     * NOTE: "Stories" has no backend support — there's no stories table,
     * entity, or endpoint in the API. This was a pure design-mockup feature.
     * Left as an empty list (rather than fake sample data) now that the
     * screen is wired to real data; remove the StoryRow UI or build a real
     * backend feature for it when ready.
     */
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                (_feedState.value as? FeedState.Success)?.let {
                    _feedState.value = it.copy(isRefreshing = true)
                }
            } else {
                _feedState.value = FeedState.Loading
            }

            _currentPage.value = 1
            when (val result = postsRepository.getFeed(page = 1, limit = 20)) {
                is ApiResult.Success -> _feedState.value = FeedState.Success(posts = result.data)
                is ApiResult.Error   -> _feedState.value = FeedState.Error(result.message)
            }
        }
    }

    fun loadMorePosts() {
        if (_isLoadingMore.value) return
        val current = _feedState.value as? FeedState.Success ?: return

        viewModelScope.launch {
            _isLoadingMore.value = true
            val nextPage = _currentPage.value + 1
            when (val result = postsRepository.getFeed(page = nextPage, limit = 20)) {
                is ApiResult.Success -> {
                    if (result.data.isNotEmpty()) _currentPage.value = nextPage
                    _feedState.value = current.copy(posts = current.posts + result.data)
                }
                is ApiResult.Error -> { /* keep existing posts; silently stop paging on error */ }
            }
            _isLoadingMore.value = false
        }
    }

    /** Optimistic update — flips immediately, then confirms against the API; rolls back on failure. */
    fun reactToPost(post: Post, reaction: ReactionType) {
        val current = _feedState.value as? FeedState.Success ?: return
        val previousPosts = current.posts

        val updated = current.posts.map { p ->
            if (p.id != post.id) return@map p
            val newReactions = p.reactions.toMutableMap()
            if (p.userReaction == reaction) {
                newReactions[reaction] = (newReactions[reaction] ?: 1) - 1
                if ((newReactions[reaction] ?: 0) <= 0) newReactions.remove(reaction)
                p.copy(reactions = newReactions, userReaction = null)
            } else {
                p.userReaction?.let { old ->
                    newReactions[old] = (newReactions[old] ?: 1) - 1
                    if ((newReactions[old] ?: 0) <= 0) newReactions.remove(old)
                }
                newReactions[reaction] = (newReactions[reaction] ?: 0) + 1
                p.copy(reactions = newReactions, userReaction = reaction)
            }
        }
        _feedState.value = current.copy(posts = updated)

        viewModelScope.launch {
            val result = postsRepository.reactToPost(post.id, reaction)
            if (result is ApiResult.Error) {
                val stateNow = _feedState.value as? FeedState.Success ?: return@launch
                _feedState.value = stateNow.copy(posts = previousPosts)
            }
        }
    }

    /**
     * NOTE: no bookmark endpoint exists on the backend yet — this stays a
     * client-side-only toggle (won't persist across app restarts) until
     * one is added.
     */
    fun bookmarkPost(post: Post) {
        val current = _feedState.value as? FeedState.Success ?: return
        _feedState.value = current.copy(
            posts = current.posts.map { p ->
                if (p.id == post.id) p.copy(isBookmarked = !p.isBookmarked) else p
            },
        )
    }
}
