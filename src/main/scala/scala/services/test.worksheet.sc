import io.vertx.ext.web.Router
import io.vertx.core.Vertx
import scala.db.mySqlClient
import scala.services
import scala.utils.auth
import scala.services.authenticationService
import org.mindrot.jbcrypt.BCrypt
import io.vertx.scala.core.JsonObject

object AuthRoutes {
  def mkRoutes(app: Router, vertx: Vertx) = {
    val client = mySqlClient.initalizeClient(vertx)

    app
      .put("/invalidateRefresh")
      .handler(ctx => {
        println("Invalidate")
        val body  = ctx.getBodyAsJson
        val token = body.getString("token")
        val user  = authenticationService.authenticate(token, ctx)

        ctx.json(JsonObject("status" -> "OK"))
      })

    app
      .post("/")
      .handler(ctx => {
        println("Inisde")
        // val token    = auth.retrieveTokenFromHeader(ctx)
        val body     = ctx.getBodyAsJson
        val username = body.getString("username")
        val password = body.getString("password")

        // val user = authenticationService.get_user_credentials(ctx, username)

        val user = authenticationService.get_user_credentials(ctx, username)
        println(user)
        /*
        val valid = BCrypt.checkpw(password, user.password)
        if (!valid) ctx.fail(401)
        val access = authenticationService.createJwtToken(user.username, 15)
        println(access)
        val refresh = authenticationService.createJwtToken(user.username, 30)
        println(refresh)
         */

        val jwt = for {
          user: Map[String, String] <- authenticationService.get_user_credentials(ctx, username)
          valid                     <- BCrypt.checkpw(password, user.get("password").get)
          access                    <- authenticationService.createJwtToken(user.username, 15)
          refresh                   <- authenticationService.createJwtToken(user.username, 30)
        } yield ctx.json(JsonObject("access" -> access, "refresh" -> refresh))

        // ctx.json(JsonObject("access" -> access, "refresh" -> refresh))

        // BCrypt.checkpw(password, passwordHash)

        // val test  = authenticationService.authenticate(token, ctx)
        // println(test)
        ctx.response.end("hi")
      })

    app
  }
}
