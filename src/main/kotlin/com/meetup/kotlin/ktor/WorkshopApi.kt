package com.meetup.kotlin.ktor

import com.meetup.kotlin.ktor.domain.Book
import com.meetup.kotlin.ktor.repository.BookRepository
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import mu.KotlinLogging
import java.util.*

fun Route.book(repository: BookRepository, simpleJwt: SimpleJwt) {

    val logger = KotlinLogging.logger {  }

    post("signup") {
        val request = call.receive<User>()
        val user = users.getOrPut(request.name) { User(request.name, request.password) }
        if (user.password != request.password) {
            error("User already exists")
        }
        call.respond(mapOf("token" to simpleJwt.sign(user.name)))
    }

    route("books") {
        get("/") {
            call.respond(HttpStatusCode.OK)
        }

        authenticate {
            post("/") {
                try {
                    var book = call.receive<Book>()
                    book = book.copy(id = UUID.randomUUID().toString())
                    repository.create(book)
                    logger.error(call.principal<UserIdPrincipal>()?.name!!)
                    call.respond(HttpStatusCode.Created, book)
                } catch (e: Exception) {
                    logger.error("Failed to perform insert", e)
                }
            }
        }

        get("/{bookId}") {
            val id = call.parameters["bookId"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val book = repository.read(id)
            if (book == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, book)
            }
        }
    }
}