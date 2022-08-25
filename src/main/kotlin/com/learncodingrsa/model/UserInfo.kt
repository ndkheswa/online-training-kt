package com.learncodingrsa.model

data class UserInfo(
    val username: String,
    val emailAddress: String,
    val password: String
)

data class UserInfoResponse(
    val username: String,
    val emailAddress: String
)

data class LoginInfo(
    val username: String,
    val emailAddress: String,
    var newPasswordRequired: Boolean

)