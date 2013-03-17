package net.unified

import org.vertx.java.core.{Handler, Vertx}
import org.vertx.java.core.http.{HttpServerResponse, RouteMatcher, HttpServerRequest}
import com.google.inject.Inject
import rest.SubscriptionListHandler.ServiceTopic
import rest.{SubscriptionListHandler, ServiceDetailsHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.vertx.java.core.buffer.Buffer
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import com.fasterxml.jackson.core.`type`.TypeReference

/**
 * Created with IntelliJ IDEA.
 * User: eprystupa
 * Date: 3/13/13
 * Time: 7:37 PM
 */

class DashboardWebServer @Inject()(serviceDetails: ServiceDetailsHandler,
                                   subscriptions: SubscriptionListHandler) {

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def create(vertx: Vertx) = {

    val routes = new RouteMatcher()

    routes.get("/services/:id", RestHandler.get("id", p => serviceDetails.get(p)))
    routes.get("/subscriptions", RestHandler.get(() => subscriptions.get()))
    routes.post("/subscriptions", RestHandler.post[ServiceTopic](p => subscriptions.post(p), new TypeReference[ServiceTopic] {}))
    routes.delete("/subscriptions/:id", RestHandler.delete("id", id => subscriptions.del(id)))

    routes.getWithRegEx(".*", new Handler[HttpServerRequest] {
      def handle(request: HttpServerRequest) {
        val file = if (request.uri == "/") "index.html" else request.uri
        request.response.sendFile(s"dashboard/src/main/site/$file")
      }
    })

    vertx.createHttpServer().requestHandler(routes)
  }

  private object RestHandler {

    def get[T](handler: () => Option[T]) = {
      new Handler[HttpServerRequest] {
        def handle(request: HttpServerRequest) {

          val result = handler()
          respond(request.response, result)
        }
      }
    }

    def get[T](param: String, handler: (String) => Option[T]) = {
      new Handler[HttpServerRequest] {
        def handle(request: HttpServerRequest) {

          val result = handler(request.params().get(param))
          respond(request.response, result)
        }
      }
    }

    def post[TInput](handler: (TInput) => Any, typeRef: TypeReference[TInput]) = new Handler[HttpServerRequest] {
      def handle(request: HttpServerRequest) {
        request.bodyHandler(new Handler[Buffer] {
          def handle(body: Buffer) {
            val input = mapper.readValue[TInput](body.getBytes, typeRef)
            val result = handler(input)
            respond(request.response, Some(result))
          }
        })
      }
    }

    def delete(param: String, handler: (String) => Unit) = new Handler[HttpServerRequest] {
      def handle(request: HttpServerRequest) {
        handler(request.params().get(param))
        request.response.end()
      }
    }

    private def respond[T](response: HttpServerResponse, result: Option[T]) {
      result match {
        case Some(value) => response.end(new Buffer(mapper.writeValueAsBytes(value)))
        case None => {
          response.statusCode = HttpResponseStatus.NOT_FOUND.getCode
          response.statusMessage = HttpResponseStatus.NOT_FOUND.getReasonPhrase
          response.end()
        }
      }

    }
  }

}
