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

object UserRoutes:
  def mkRoutes(app: Router, vertx: Vertx): Router = {

    val client = mySqlClient.initalizeClient(vertx)

    app
      .get("/")
      .handler(ctx => {
        ctx.response.end("hi")
      })

    app
      .post("/")
      .produces("application/json")
      .handler(ctx => {
        println("Inside register")
        val objectMapper  = ObjectMapper()
        val defaultMapper = objectMapper.registerModule(DefaultScalaModule)
        val body          = ctx.getBodyAsJson

        val password = body.getString("password")
        val username = body.getString("username")
        val surname  = body.getString("surname")
        val name     = body.getString("name")

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        println(passwordHash)

        client
          .preparedQuery(
            "INSERT INTO users (username, password, name, surname, created_at) VALUES (?, ?, ?, ?, ?)"
          )
          .execute(
            Tuple.of(username, passwordHash, name, surname, DateTime()),
            ar => {
              if (ar.succeeded()) {
                val rows         = ar.result()
                val lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID);

                ctx
                  .response()
                  .setStatusCode(200)
                  .putHeader("content-type", "application/json")
                  .end(JsonObject("status" -> "OK", "user_id" -> lastInsertId).toBuffer())

                println(ar.result())

                val user: UserEntity = UserEntity(
                  username = rows.head.getString("id"),
                  password = rows.head.getString("password"),
                  name = rows.head.getString("name"),
                  surname = rows.head.getString("surname"),
                  createdAt =
                    DateTime.parse(rows.head.getString("created_at"), DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"))
                )

                val strUser = defaultMapper.writeValueAsString(user)
                println(strUser)

                ctx
                  .response()
                  .putHeader("content-type", "application/json")
                  .setStatusCode(200)
                  .end(strUser)
              } else {
                println("Failure: " + ar.cause().getMessage());
                ctx.response.end("Something went wong")
              }
            }
          )
      })

    app
      .delete("/users/:id")
      .handler(ctx => {
        ctx.response.end("Created user")
      })

    app
  }
