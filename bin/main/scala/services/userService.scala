package scala.services
import scala.db.mySqlClient
import io.vertx.core.Vertx
import io.vertx.sqlclient.Tuple
import io.vertx.mysqlclient.MySQLClient
import org.joda.time.DateTime
import scala.gradle.example.Models._
import io.vertx.ext.web.RoutingContext
import io.vertx.scala.core.JsonObject
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import collection.convert.ImplicitConversions._
import org.joda.time.format.DateTimeFormat
import io.vertx.ext.auth.User
import scala.concurrent.Awaitable
import scala.concurrent.Await
import scala.collection.JavaConverters._
import scala.util.Sorting
import scala.concurrent.Promise
import java.util.Iterator
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import io.vertx.lang.scala.VertxFutureConverter
import scala.util.Success
import scala.util.Failure
import scala.concurrent.duration.Duration
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Row
import io.vertx.core.spi.launcher.ExecutionContext

object userService {
  val vertx         = Vertx.vertx()
  val client        = mySqlClient.initalizeClient(vertx)
  val objectMapper  = ObjectMapper()
  val defaultMapper = objectMapper.registerModule(DefaultScalaModule)

  def insert_user_one(
      username: String,
      passwordHash: String,
      name: String,
      surname: String,
      ctx: RoutingContext
  ): Option[Long] = {
    val query = client
      .preparedQuery(
        "INSERT INTO users (username, password, name, surname, created_at) VALUES (?, ?, ?, ?, ?)"
      )
      .execute(
        Tuple.of(username, passwordHash, name, surname, DateTime())
      )
      .asScala

    val rows         = Await.result(query, Duration.Inf)
    val lastInsertId = rows.property(MySQLClient.LAST_INSERTED_ID)
    Some(lastInsertId)
  }
  def get_user_by_id(ctx: RoutingContext, id: Int): Option[UserEntity] = {
    val query = client
      .preparedQuery(
        "SELECT username, surname, name, created_at, password FROM users WHERE id = ? LIMIT 1"
      )
      .execute(Tuple.of(id))
      .asScala
    val value = Await.result(query, Duration.Inf)
    val user = UserEntity(
      username = value.head.getString("username"),
      surname = value.head.getString("surname"),
      name = value.head.getString("name"),
      password = Some(value.head.getString("password"))
    )
    Some(user)
  }

  def get_all_users(ctx: RoutingContext): Seq[UserEntity] = {
    val query     = client.query("SELECT * FROM users").execute().asScala
    val value     = Await.result(query, Duration.Inf)
    val resultSet = value.iterator
    if (!resultSet.hasNext) {
      ctx.response().setStatusCode(404).end("No user found")
    }
    val users: Seq[UserEntity] =
      resultSet.asScala.toSeq
        .map(user =>
          UserEntity(
            username = user.getString("username"),
            surname = user.getString("surname"),
            name = user.getString("name")
          )
        )
    users
  }
  def update_user_by_id() = {}
  def delete_user_by_id() = {}

  def await[T](awaitable: Awaitable[T]): T = {
    import scala.concurrent.duration._
    Await.result(awaitable, 5.seconds)
  }

}
