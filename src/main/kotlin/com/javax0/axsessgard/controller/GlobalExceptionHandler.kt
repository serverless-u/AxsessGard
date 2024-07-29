package com.javax0.axsessgard.controller

import com.google.gson.Gson
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import java.time.Instant

data class ErrorResponse(val status: Int, val error: String, val message: String, val timestamp: Long)


@ControllerAdvice
class GlobalExceptionHandler {

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
}

class ApplicationException(
    val status: HttpStatus,
    override val message: String
) : RuntimeException(message)
