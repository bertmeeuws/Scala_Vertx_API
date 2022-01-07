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
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import io.vertx.ext.web.RoutingContext
import scala.gradle.example.Models._
import io.vertx.sqlclient.Tuple
import scala.collection.JavaConverters._
import io.vertx.lang.scala.VertxFutureConverter
import collection.convert.ImplicitConversions._
import scala.utils.auth
import java.time.LocalDateTime
import java.time.ZoneOffset
import scala.utils.auth._
import io.vertx.lang.scala.*
import scala.util.Success
import scala.util.Failure
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.jwt.authorization.JWTAuthorization
import io.vertx.core.json.JsonArray

object authenticationService {
  val vertx  = Vertx.vertx()
  val client = mySqlClient.initalizeClient(vertx)

  def createJwtToken(username: String, minutes: Int): Option[String] = {
    val date = getJwtExpiryUnixEpoch(15)
    val authInfo: JsonObject = JsonObject(
      "sub"         -> username,
      "expTime"     -> (minutes.toString + "min"),
      "permissions" -> JsonArray(List("user", "admin"))
    )
    val token = scala.config.jwt.provider
      .generateToken(authInfo, JWTOptions().setExpiresInMinutes(minutes))
    Some(token)
  }

  def createRefreshToken(username: String): String = {
    val date                 = DateTime(new Date()).plusMinutes(30)
    val authInfo: JsonObject = JsonObject("sub" -> username, "exp" -> date)
    val token                = scala.config.jwt.provider.generateToken(authInfo)
    return token
  }

  def generateToken(username: String, password: String): String = {
    val authInfo: JsonObject = JsonObject("sub" -> username, "exp" -> "15")
    val token                = scala.config.jwt.provider.generateToken(authInfo)
    return token
  }

  case class UserCredentials(username: String, password: String) {
    def toMap: Map[String, String] = Map(
      "username" -> username,
      "password" -> password
    )
  }

  def get_user_credentials(ctx: RoutingContext, username: String): Option[UserCredentials] = {
    val query = client
      .preparedQuery(
        "SELECT username, password FROM users WHERE username = ? LIMIT 1"
      )
      .execute(Tuple.of(username))
      .asScala
    val value = Await.result(query, Duration.Inf)
    if (!value.iterator.hasNext) {
      return None
    }
    val creds = UserCredentials(
      value.head.getString(0),
      value.head.getString(1)
    )
    Some(creds)

    // Turning into a map
    // creds.toMap
  }

  def update_user_refresh_token(ctx: RoutingContext, username: String, refresh: String): Option[Boolean] = {
    val query = client
      .preparedQuery(
        "UPDATE users SET refresh_token = ? WHERE username=?"
      )
      .execute(Tuple.of(refresh, username))
      .asScala
    val value = Await.result(query, Duration.Inf)
    if (!value.iterator.hasNext) {
      return None
    }
    Some(true)
  }

  def get_refresh_token_by_username(ctx: RoutingContext, username: String): Option[String] = {
    val query = client
      .preparedQuery(
        "SELECT refresh_token WHERE username=? LIMIT=1"
      )
      .execute(Tuple.of(username))
      .asScala
    val value = Await.result(query, Duration.Inf)
    if (!value.iterator.hasNext) {
      ctx.fail(401)
      return None
    }
    Some(value.head.getString(0))
  }

  def get_roles_from_user(user: User): Option[List[String]] = {
    Some(user.attributes().getJsonObject("accessToken").getJsonArray("permissions").toList.map(_.toString))
  }

  def protect_route_with_roles(ctx: RoutingContext, roles: List[String], allowed_roles: List[String]): Unit = {}

  def authenticate(token: String, ctx: RoutingContext): Option[User] = {
    val authenticate =
      scala.config.jwt.provider
        .authenticate(
          JsonObject("token" -> token)
        )
        .asScala

    val result = Await.result(authenticate, Duration.Inf)

    result match {
      case user: User =>
        val roles = get_roles_from_user(user).getOrElse(List())
        println(user)
        Some(user)
      case null =>
        println("fail")
        ctx.fail(401)
        return None
    }

  }

  def me() = {}

}
