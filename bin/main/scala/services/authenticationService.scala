package scala.services
import scala.db.mySqlClient
import io.vertx.ext.auth.authentication
import io.vertx.ext.auth.AuthProvider
import io.vertx.scala.core.JsonObject
import com.fasterxml.jackson.databind.util.JSONPObject
import scala.concurrent.Future
import io.vertx.ext.auth.User
import io.vertx.scala.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.auth.jwt.JWTAuth

import io.vertx.scala.ext.auth.KeyStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.scala.ext.auth.PubSecKeyOptions
import scala.config
import org.joda.time.DateTime
import java.util.Date

/*
object authProvider extends AuthProvider {
  def authenticate(credentials: JsonObject): Future[User] = {

    return
  }
}
 */

object authenticationService {
  def createAccessToken(username: String): String = {
    val date = DateTime(new Date()).plusMinutes(15)

    val authInfo: JsonObject = JsonObject("sub" -> username, "exp" -> date)
    val token                = scala.config.jwt.provider.generateToken(authInfo)
    return token
  }

  def createRefreshToken(username: String): String = {
    val date                 = DateTime(new Date()).plusMinutes(30)
    val authInfo: JsonObject = JsonObject("sub" -> username, "exp" -> date)
    val token                = scala.config.jwt.provider.generateToken(authInfo)
    return token
  }

  def login(username: String, password: String): String = {
    val authInfo: JsonObject = JsonObject("sub" -> username, "exp" -> "15")
    val token                = scala.config.jwt.provider.generateToken(authInfo)
    return token
  }

  def authenticate(token: String) = {
    // Check accesstoken first
    scala.config.jwt.provider
      .authenticate(
        JsonObject("token" -> token, "options" -> JsonObject("ignoreExpiration" -> false))
      )
      .onSuccess { case user =>
        println("Yes")
        println("User: " + user.principal())
      // Go to request
      }
      .onFailure { case err =>
        println(err)
        println("error")
      }

    // Check for refresh token

  }

  def me() = {}

}
