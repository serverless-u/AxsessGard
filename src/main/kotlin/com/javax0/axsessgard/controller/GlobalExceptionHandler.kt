package com.javax0.axsessgard.controller

import com.auth0.jwt.JWT
import com.google.gson.Gson
import com.javax0.axsessgard.service.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.Instant

data class ErrorResponse(val status: Int, val error: String, val message: String, val timestamp: Long)


@ControllerAdvice
class GlobalExceptionHandler(
    private val jwtService: JwtService
) {

    @Value("\${axsg.issuer}")
    private lateinit var issuer: String

    @Value("\${axsg.ttl}")
    private lateinit var ttlString: String

    private val ttl: Int
        get() = ttlString.toInt()

    private val gson = Gson()

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, request: WebRequest): ResponseEntity<String> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = ex.localizedMessage ?: "An unexpected error occurred",
            timestamp = Instant.now().toEpochMilli()
        )
        val jsonResponse = gson.toJson(errorResponse)
        return ResponseEntity(jsonResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(ApplicationException::class)
    fun handleMyCustomException(ex: ApplicationException, request: WebRequest): ResponseEntity<String> {
        val errorResponse = ErrorResponse(
            status = ex.status.value(),
            error = ex.status.reasonPhrase,
            message = ex.localizedMessage,
            timestamp = Instant.now().toEpochMilli()
        )
        val jsonResponse = gson.toJson(errorResponse)
        return ResponseEntity(jsonResponse, ex.status)
    }

    @ExceptionHandler(RequestAuthorization::class)
    fun handleMyCustomException(ex: RequestAuthorization, request: WebRequest): ResponseEntity<String> {
        val locationUrl =
            (request.getHeader("x-forwarded-for")?.let {
                ServletUriComponentsBuilder.fromUriString(it)
            }
                ?: ServletUriComponentsBuilder.fromCurrentContextPath())
                .pathSegment("axsg")
                .pathSegment("permissions")
                .toUriString()



        request.getHeader("x-user-id")?.let { userId ->
            val errorResponse = ErrorResponse(
                status = HttpStatus.UNAUTHORIZED.value(),
                error = HttpStatus.UNAUTHORIZED.reasonPhrase,
                message = "User '$userId' needs authorization. Ask for permission at $locationUrl using the request token from the header 'X-Request-Authorization'",
                timestamp = Instant.now().toEpochMilli()
            )
            val requestToken = jwtService.sign(
                JWT.create().withIssuer(issuer)
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(Instant.now().plusSeconds(ttl.toLong()))
                    .withSubject(userId)
                    .withClaim("policy", ex.policy)
                    .withClaim("roles", ex.roles)
            )
            val headers = HttpHeaders().apply {
                set(HttpHeaders.LOCATION, locationUrl)
                set("X-Request-Authorization", requestToken)
            }
            val jsonResponse = gson.toJson(errorResponse)
            return ResponseEntity(jsonResponse, headers, HttpStatus.UNAUTHORIZED)
        }
        return ResponseEntity(
            gson.toJson(
                ErrorResponse(
                    status = HttpStatus.FORBIDDEN.value(),
                    error = HttpStatus.FORBIDDEN.reasonPhrase,
                    message = "Accessing the resource needs authentication and authorization",
                    timestamp = Instant.now().toEpochMilli()
                )
            ), HttpStatus.FORBIDDEN
        )
    }
}

class ApplicationException(
    val status: HttpStatus,
    override val message: String
) : RuntimeException(message)

class RequestAuthorization(
    override val message: String,
    val policy: String,
    val roles: List<String>
) : RuntimeException(message)