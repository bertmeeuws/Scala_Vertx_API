package scala.gradle.example.routes

import io.vertx.ext.web.Router
import io.vertx.core.Vertx

object UserRoutes:
  def mkRoutes(app: Router): Router =
    app
      .get("/users")
      .handler(ctx => {
        ctx.response.end("hi")
      })
    app
