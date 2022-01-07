package scala.gradle.example.routes

import io.vertx.ext.web.Router
import io.vertx.core.Vertx
import scala.db.mySqlClient
import io.vertx.sqlclient.Tuple
import collection.convert.ImplicitConversions._
import scala.gradle.example.Perfume
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scala.gradle.example.Models._
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import org.joda.time.format.DateTimeFormat
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.scala.core.JsonObject
import io.vertx.mysqlclient.MySQLClient
import scala.services.userService
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.fasterxml.jackson.module.scala.util.PimpedType.UnwrapPimpedType
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Row
import scala.util.Success
import scala.util.Failure
import scala.collection.JavaConverters._
import io.vertx.core.json.JsonArray
import scala.services.authenticationService

object UserRoutes:
  def mkRoutes(vertx: Vertx): Router = {

    val client = mySqlClient.initalizeClient(vertx)

    val objectMapper  = ObjectMapper()
    val defaultMapper = objectMapper.registerModule(DefaultScalaModule)

    val app = Router.router(vertx)

    app
      .get("/")
      .handler(ctx => {
        // val id = ctx.pathParam("id")
        val authHeader = ctx.request.getHeader("Authorization")
        // Checking for JWT
        if (authHeader.startsWith("Bearer ")) {
          val token = authHeader.substring(7, authHeader.length);
          println("Found token")
          val jwt = for {
            user    <- authenticationService.authenticate(token, ctx)
            access  <- authenticationService.createJwtToken(user.principal.getString("username"), 15)
            refresh <- authenticationService.createJwtToken(user.principal.getString("username"), 30)
          } ctx.json(JsonObject("access" -> access, "refresh" -> refresh))
        } else {
          // Error
        }

        println("Inside get users")
        val users: Seq[UserEntity] = userService.get_all_users(ctx)
        val usersAsJson = JsonArray(
          users.map(user =>
            JsonObject(
              "username" -> user.username,
              "surname"  -> user.surname,
              "name"     -> user.name
            )
          )
        )
        ctx.json(usersAsJson)
      })

    /*
    app
      .get("/:id")
      .handler(ctx => {
        val id: Int = ctx.pathParam("id").toInt
        val user    = userService.get_user_by_id(ctx, id)

        ctx.json(
          JsonObject(
            "username" -> user.map(_.username),
            "surname"  -> user.map(_.surname),
            "name"     -> user.map(_.name)
          )
        )
      })
     */

    app
      .post("/")
      .produces("application/json")
      .handler(ctx => {
        println("Inside insert user")
        val body     = ctx.getBodyAsJson
        val password = body.getString("password")
        val username = body.getString("username")
        val surname  = body.getString("surname")
        val name     = body.getString("name")

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        val insertedUser = for {
          id: Long <- userService.insert_user_one(username, passwordHash, name, surname, ctx)
          user     <- userService.get_user_by_id(ctx, id.toInt)
          access   <- authenticationService.createJwtToken(user.username, 15)
          refresh  <- authenticationService.createJwtToken(user.username, 30)
        } yield JsonObject(
          "username" -> user.username,
          "surname"  -> user.surname,
          "name"     -> user.name,
          "access"   -> access,
          "refresh"  -> refresh
        )

        println(insertedUser.getOrElse(ctx.fail(404)))

        ctx.json(insertedUser.getOrElse(ctx.fail(404)))

      })

    app
      .delete("/users/:id")
      .handler(ctx => {
        ctx.response.end("Created user")
      })

    app
  }
