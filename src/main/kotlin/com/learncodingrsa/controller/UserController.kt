package com.learncodingrsa.controller

import com.learncodingrsa.authentication.AuthenticationService
import com.learncodingrsa.model.UserInfo
import com.learncodingrsa.model.UserInfoResponse
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/")
class UserController(@ConfigProperty(name = "cognito.pool-id") private val poolId: String) {

    @Inject
    private lateinit var authenticationService: AuthenticationService

    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    fun findUser(email: String): UserInfoResponse? {
        return authenticationService.findUserByEmail(email)
    }

    @POST
    @Path("register")
    @Produces(MediaType.TEXT_HTML)
    fun register(userInfo: UserInfo): AdminCreateUserResponse? {
        return authenticationService.createUser(userInfo)
    }
}