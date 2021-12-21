package scala.gradle.example
import scala.concurrent.Promise
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.core.Vertx
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.VertxFutureConverter
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Await
import concurrent.duration.DurationInt
import io.vertx.scala.mysqlclient.MySQLConnectOptions
import io.vertx.mysqlclient.MySQLPool
import io.vertx.scala.sqlclient.PoolOptions
import io.vertx.mysqlclient.MySQLPool
import io.vertx.scala.mysqlclient.MySQLConnectOptions
import collection.convert.ImplicitConversions._
import io.vertx.lang.scala.json.Json
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scala.gradle.example.routes.UserRoutes

val PORT = 8000

case class Perfume(id: Int, name: String, description: String)

class HttpVerticle extends ScalaVerticle {
  override def start(promise: Promise[Unit]): Unit = {
    // Create a router

    val app: Router = Router.router(vertx)
    app.mountSubRouter("/api", UserRoutes.mkRoutes(app))

    val connectOptions =
      MySQLConnectOptions(port = 3306, host = "localhost", database = "perfume", user = "root", password = "root")

    val poolOptions = PoolOptions(5)

    val client = MySQLPool.pool(vertx, connectOptions, poolOptions);
    // Sets up JSON parsing
    app.route().handler(BodyHandler.create)

    app
      .get("/hello")
      .handler(ctx => ctx.response.end("world"))

    app
      .post("/send-json")
      .handler(ctx => {
        val params = ctx.getBodyAsJson
        val name   = params.getString("name")
        val test = client.query("SELECT * FROM perfumes").execute { ar =>
          {
            if (ar.succeeded()) {
              val rows = ar.result();

              val perfumes = rows.map(row => {
                Perfume(
                  id = row.getInteger("id"),
                  name = row.getString("name"),
                  description = row.getString("description")
                )
              })

              val objectMapper  = ObjectMapper()
              val defaultMapper = objectMapper.registerModule(DefaultScalaModule)
              val strPerfumes   = defaultMapper.writeValueAsString(perfumes)

              ctx
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end(strPerfumes)

            } else {
              ctx
                .response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end("{ status: 'OK' }")
            }
          }
        }
      })

    vertx
      .createHttpServer()
      .requestHandler(app)
      .listen(PORT, "0.0.0.0")
      .asScala()
      .onComplete {
        case Success(_) => promise.complete(Success(()))
        case Failure(e) => promise.complete(Failure(e))
      }
  }
}

def deployVerticle[App <: ScalaVerticle](verticle: App)(using vertx: Vertx): String =
  given VertxExecutionContext(vertx, vertx.getOrCreateContext())
  println(s"Attempting to deploy HTTP app at http://localhost:$PORT")
  val deploymentPromise = vertx
    .deployVerticle(verticle.asJava)
    .asScala()
    .andThen {
      case Success(d) => d
      case Failure(t) => throw new RuntimeException(t)
    }
  val deploymentId = Await.result(deploymentPromise, 10.seconds)
  println(f"Successfully deployed verticle. Verticle deployment id = $deploymentId")
  deploymentId

def undeployVerticle(deploymentId: String)(using vertx: Vertx) =
  given VertxExecutionContext(vertx, vertx.getOrCreateContext())
  println(s"Attempting to undeploy HTTP app with ID $deploymentId")
  val deploymentPromise = vertx
    .undeploy(deploymentId)
    .asScala()
    .andThen {
      case Success(d) => d
      case Failure(t) => throw new RuntimeException(t)
    }
  Await.result(deploymentPromise, 10.seconds)

@main def App() =
  val vertx  = Vertx.vertx()
  val server = HttpVerticle()
  deployVerticle(server)(using vertx = vertx)
