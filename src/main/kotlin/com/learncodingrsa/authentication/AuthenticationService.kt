package com.learncodingrsa.authentication

import com.learncodingrsa.model.*
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.*
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class AuthenticationService(private val client: CognitoIdentityProviderClient,
                            @ConfigProperty(name = "cognito.pool-id") private val poolId: String,
                            @ConfigProperty(name = "cognito.client-id") private val clientId: String) {

    fun createUser(userInfo: UserInfo): AdminCreateUserResponse? {
        var resp: AdminCreateUserResponse? = null
        try {
            val emailAddress: String? = userInfo.let { it.emailAddress }

            if (!emailAddress.isNullOrEmpty()) {

                val info = findUserByEmail(emailAddress)
                if (info == null) {
                    resp = client.adminCreateUser(
                        AdminCreateUserRequest
                            .builder()
                            .userPoolId(poolId)
                            .username(userInfo.let { it.username })
                            .temporaryPassword( userInfo.let { it.password  }  )
                            .userAttributes(AttributeType.builder().name("email").value(emailAddress).build())
                            .build()
                    )
                    val user: UserType = resp.user()
                    val userAttr: List<AttributeType> = user.attributes()

                    if (user != null) {

                    }
                }
            }
        } catch (e: CognitoIdentityProviderException) {
            e.awsErrorDetails().errorMessage();
        }
        return resp
    }

    fun login(loginRequest: LoginRequest): LoginInfo? {
        var info: LoginInfo? = null
        var newPasswordRequired: Boolean = false

        try {
            var sessionInfo: SessionInfo? = sessionHandler(loginRequest.username, loginRequest.password)

            if (sessionInfo != null) {
                val userInfo: UserInfoResponse? = getUserInfo(loginRequest.username)
                info = LoginInfo(userInfo!!.username, userInfo.emailAddress, newPasswordRequired)

                val challengeResult: String = sessionInfo.challengeResult
                if (!challengeResult.isNullOrEmpty()) {
                    info.newPasswordRequired = challengeResult == ChallengeNameType.NEW_PASSWORD_REQUIRED.name
                }

            }
        } catch (e: CognitoIdentityProviderException) {
            e.awsErrorDetails().errorMessage()
        }
        return info
    }

    fun getUserInfo(username: String): UserInfoResponse? {
        var info: UserInfoResponse? = null

        try {
            val userResponse = client.adminGetUser(
                AdminGetUserRequest.builder()
                    .userPoolId(poolId)
                    .username(username)
                    .build()
            )

            val userAttr: List<AttributeType> = userResponse.userAttributes()
            var emailAddr : String? = null

            for (attr: AttributeType in userAttr) {
                if (attr.name().equals(EMAIL)) {
                    emailAddr = attr.value()
                }
            }

            val responseUsername: String = userResponse.username()
            if (!responseUsername.isNullOrEmpty()) {
                info = emailAddr?.let { UserInfoResponse( responseUsername, it ) }
            }

        } catch (e: CognitoIdentityProviderException) {
            e.awsErrorDetails().errorMessage()
        }
        return info
    }

    fun sessionHandler(username: String, password: String): SessionInfo? {
        var info: SessionInfo? = null

        val authParams = mutableMapOf<String, String>()
        authParams["USERNAME"] = username
        authParams["PASSWORD"] = password

        try {
            val authResult = client.adminInitiateAuth(
                AdminInitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .userPoolId(poolId)
                    .clientId(clientId)
                    .authParameters(authParams)
                    .build()
            )

            if (authResult != null) {
                val session = authResult.session()
                var accessToken: String? = null
                val resultType = authResult.authenticationResult()
                accessToken = if (resultType != null) {
                    resultType.accessToken()
                } else {
                    ""
                }
                val challengeResult = authResult.challengeName().name
                info = SessionInfo(session, accessToken!!, challengeResult)
            }

        } catch (e: CognitoIdentityProviderException) {
            e.awsErrorDetails().errorMessage()
        }
        return info
    }

    fun findUserByEmail(email: String): UserInfoResponse? {
        var info: UserInfoResponse?  = null;

        if (!email.isNullOrEmpty()) {

            val emailQuery: String = "email=\"$email\""
            try {
                val resp: ListUsersResponse = client.listUsers(
                    ListUsersRequest.builder()
                        .userPoolId(poolId)
                        .attributesToGet("email")
                        .filter(emailQuery)
                        .build()

                )
                val users = resp.users()
                if (!users.isNullOrEmpty()) {
                    if (users.size == 1) {
                        val user = users[0]
                        val username = user.username()
                        var emailAddress: String? = null;
                        val attributes: List<AttributeType>? = user.attributes()
                        if (!attributes.isNullOrEmpty()) {
                            for (attr: AttributeType in attributes) {
                                if (attr.name().equals("email"))
                                    emailAddress = attr.value()
                            }
                            if (!username.isNullOrEmpty())
                                info = emailAddress?.let { UserInfoResponse(username, it) }
                        }
                    }

                }

            } catch (e: CognitoIdentityProviderException) {
                e.awsErrorDetails().errorMessage()
            }
        }
        return  info
    }

    companion object {
        private val EMAIL: String = "email"
        private val USERNAME: String = "username"
        private val PASSWORD: String = "password"
        private val NEW_PASSWORD: String = "new_password"
    }

}