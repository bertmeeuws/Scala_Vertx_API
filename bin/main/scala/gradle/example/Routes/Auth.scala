package scala.gradle.example.routes

import io.vertx.ext.web.Router
import io.vertx.core.Vertx
import scala.db.mySqlClient
import scala.services
import scala.utils.auth
import scala.services.authenticationService
import org.mindrot.jbcrypt.BCrypt
import io.vertx.scala.core.JsonObject
import scala.services.authenticationService.UserCredentials

object AuthRoutes {
  def mkRoutes(vertx: Vertx) = {
    val client = mySqlClient.initalizeClient(vertx)
    val app    = Router.router(vertx)

    app
      .get("/")
      .handler(ctx => {
        println("Inside auth get")
        ctx.json(JsonObject("Status" -> "OK"))
      })

    app
      .put("/invalidateRefresh")
      .handler(ctx => {
        val body  = ctx.getBodyAsJson
        val token = body.getString("token")

        val jwt = for {
          user <- authenticationService.authenticate(token, ctx)
          is_refresh_in_db <- authenticationService
            .get_refresh_token_by_username(ctx, user.principal().getString("sub"))
          access  <- authenticationService.createJwtToken(user.principal.getString("username"), 15)
          refresh <- authenticationService.createJwtToken(user.principal.getString("username"), 30)
          insert_new_refresh <- authenticationService
            .update_user_refresh_token(ctx, user.principal().getString("sub"), refresh)
        } ctx.json(JsonObject("access" -> access, "refresh" -> refresh))

      })

    app
      .post("/")
      .handler(ctx => {
        println("Inisde")
        // val token    = auth.retrieveTokenFromHeader(ctx)
        val body     = ctx.getBodyAsJson
        val username = body.getString("username")
        val password = body.getString("password")

        val jwt = for {
          user: UserCredentials <- authenticationService.get_user_credentials(ctx, username)
          valid <- BCrypt.checkpw(password, user.password) match {
            case true => Some(true)
            case false => {
              ctx.fail(401)
              None
            }
          }
          access  <- authenticationService.createJwtToken(user.username, 15)
          refresh <- authenticationService.createJwtToken(user.username, 30)
        } yield JsonObject("access" -> access, "refresh" -> refresh)

        println(jwt)

        ctx.response.end("hi")
      })

    app
  }
}
