package com.sjaindl.chatravelserver.graphql

import graphql.ExecutionInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

data class GraphQLRequest(
    val query: String,
    val operationName: String? = null,
    val variables: Map<String, Any?>? = null,
)

fun Route.graphqlRoutes(graphQL: graphql.GraphQL) {

    route("/graphql") {

        post {
            val req = call.receive<GraphQLRequest>()
            val input = ExecutionInput.newExecutionInput()
                .query(req.query)
                .operationName(req.operationName)
                .variables(req.variables ?: emptyMap())
                .build()

            val result = graphQL.execute(input)
            val spec = result.toSpecification() // Map<String, Any>

            call.respond(HttpStatusCode.OK, spec)
        }

        // GET for health
        get {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }
    }
}
