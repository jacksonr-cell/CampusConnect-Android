package com.campusconnect.app.data.remote.mapper

import com.campusconnect.app.data.model.User
import com.campusconnect.app.data.remote.dto.UserDto

fun UserDto.toDomain(): User = User(
    id         = id,
    username   = username,
    firstName  = firstName,
    lastName   = lastName,
    avatarUrl  = avatarUrl,
    university = university,
    isVerified = isVerified,
    isOnline   = false, // presence comes from the WebSocket layer, not REST
    role       = role.toUserRole(),
)
