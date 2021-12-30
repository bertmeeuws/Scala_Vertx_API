package scala.gradle.example.routes

import io.vertx.ext.web.Router
import io.vertx.core.Vertx
import scala.db.mySqlClient
import scala.services

object AuthRoutes {
  def mkRoutes(app: Router, vertx: Vertx) = {
    val client = mySqlClient.initalizeClient(vertx)

    app
      .post("/")
      .handler(ctx => {
        
        ctx.response.end("hi")
      })

    app
  }
}
