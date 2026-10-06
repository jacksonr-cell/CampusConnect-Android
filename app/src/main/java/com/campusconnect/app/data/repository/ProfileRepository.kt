package com.campusconnect.app.data.repository

import com.campusconnect.app.data.model.Post
import com.campusconnect.app.data.model.Property
import com.campusconnect.app.data.model.ProfileStats
import com.campusconnect.app.data.model.UserProfile
import com.campusconnect.app.data.remote.api.AccommodationApi
import com.campusconnect.app.data.remote.api.GamificationApi
import com.campusconnect.app.data.remote.api.PostsApi
import com.campusconnect.app.data.remote.api.UsersApi
import com.campusconnect.app.data.remote.mapper.toDomain
import com.campusconnect.app.data.remote.mapper.toEpochMillisOrNow
import com.campusconnect.app.data.remote.mapper.toLevel
import com.campusconnect.app.data.remote.mapper.toTotalPoints
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ProfileRepository(
    private val usersApi: UsersApi,
    private val gamificationApi: GamificationApi,
    private val postsApi: PostsApi,
    private val accommodationApi: AccommodationApi,
) {
    /** Fetches profile, points, and badges in parallel; points/badges degrade gracefully if either fails. */
    suspend fun getMyProfile(): ApiResult<UserProfile> = coroutineScope {
        val profileDeferred = async { safeApiCall { usersApi.getMyProfile() } }
        val pointsDeferred  = async { safeApiCall { gamificationApi.getMyPoints() } }
        val badgesDeferred  = async { safeApiCall { gamificationApi.getMyBadges() } }

        val profileResult = profileDeferred.await()
        val pointsResult   = pointsDeferred.await()
        val badgesResult   = badgesDeferred.await()

        when (profileResult) {
            is ApiResult.Error -> profileResult
            is ApiResult.Success -> {
                val userDto = profileResult.data
                val pointsDto = (pointsResult as? ApiResult.Success)?.data
                val badges = (badgesResult as? ApiResult.Success)?.data?.map { it.toDomain() } ?: emptyList()

                val stats = ProfileStats(
                    postsCount       = userDto.postsCount,
                    followersCount   = userDto.followersCount,
                    followingCount   = userDto.followingCount,
                    reputationPoints = pointsDto.toTotalPoints(),
                    level            = pointsDto.toLevel(),
                )

                ApiResult.Success(
                    UserProfile(
                        user           = userDto.toDomain(),
                        bio            = userDto.bio,
                        coverImageUrl  = userDto.coverImageUrl,
                        courseOfStudy  = userDto.courseOfStudy,
                        yearOfStudy    = userDto.yearOfStudy,
                        joinedAtMillis = userDto.createdAt.toEpochMillisOrNow(),
                        stats          = stats,
                        badges         = badges,
                        isOwnProfile   = true,
                    ),
                )
            }
        }
    }

    /** Backend gap closed — this now calls the real GET /posts/user/:userId endpoint. */
    suspend fun getMyPosts(userId: String, page: Int = 1, limit: Int = 20): ApiResult<List<Post>> =
        when (val result = safeApiCall { postsApi.getPostsByUser(userId, page, limit) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.map { it.toDomain() })
            is ApiResult.Error   -> result
        }

    suspend fun getSavedProperties(): ApiResult<List<Property>> =
        when (val result = safeApiCall { accommodationApi.getSavedProperties(1, 50) }) {
            is ApiResult.Success -> {
                val ids = result.data.data.map { it.id }.toSet()
                ApiResult.Success(result.data.data.map { it.toDomain(ids) })
            }
            is ApiResult.Error -> result
        }
}
