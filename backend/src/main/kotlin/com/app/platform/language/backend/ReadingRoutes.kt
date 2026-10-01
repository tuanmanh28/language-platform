package com.app.platform.language.backend

import com.app.platform.language.core.exam.ReadingScorer
import com.app.platform.language.core.model.BundledReadingTests
import com.app.platform.language.core.model.ReadingTest
import com.app.platform.language.core.model.SubmitAnswersRequest
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

/** Source of tests. Phase 1 serves the bundled tests; next step is PostgreSQL + an admin CMS. */
interface ContentStore {
    fun readingTests(): List<ReadingTest>
    fun readingTest(id: String): ReadingTest?
}

class BundledContentStore : ContentStore {
    override fun readingTests(): List<ReadingTest> = BundledReadingTests.all
    override fun readingTest(id: String): ReadingTest? = BundledReadingTests.find(id)
}

fun Route.readingRoutes(store: ContentStore) {
    route("/api/v1/reading/tests") {
        get {
            call.respond(store.readingTests().map { it.toSummary() })
        }

        get("/{id}") {
            val id = call.parameters.getOrFail("id")
            call.respond(store.readingTest(id) ?: throw NotFoundException("Reading test '$id' not found"))
        }

        post("/{id}/submit") {
            val id = call.parameters.getOrFail("id")
            val test = store.readingTest(id) ?: throw NotFoundException("Reading test '$id' not found")
            val request = call.receive<SubmitAnswersRequest>()
            call.respond(ReadingScorer.score(test, request.answers))
        }
    }
}
