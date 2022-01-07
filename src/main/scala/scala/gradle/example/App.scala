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
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scala.gradle.example.routes.UserRoutes
import scala.gradle.example.routes.AuthRoutes
import scala.db.mySqlClient
import scala.services.authenticationService
import io.vertx.scala.core.JsonObject
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import scala.config._

val PORT = 8000

case class Perfume(id: Int, name: String, description: String)

class HttpVerticle extends ScalaVerticle {
  override def start(promise: Promise[Unit]): Unit = {
    // Create a router
    val app: Router = Router.router(vertx)
    app
      .route()
      .handler(
        CorsHandler
          .create(".*.")
          .allowedMethod(io.vertx.core.http.HttpMethod.GET)
          .allowedMethod(io.vertx.core.http.HttpMethod.POST)
          .allowedMethod(io.vertx.core.http.HttpMethod.DELETE)
          .allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
          .allowCredentials(true)
          .allowedHeader("Access-Control-Request-Method")
          .allowedHeader("Access-Control-Allow-Credentials")
          .allowedHeader("Access-Control-Allow-Origin")
          .allowedHeader("Access-Control-Allow-Headers")
          .allowedHeader("Authorization")
          .allowedHeader("Content-Type")
      );
    app.route().handler(BodyHandler.create)
    app.mountSubRouter("/api/auth", AuthRoutes.mkRoutes(vertx))
    app.mountSubRouter("/api/users", UserRoutes.mkRoutes(vertx))

    // Setting up security

    app.route("/").handler(JWTAuthHandler.create(jwt.provider));

    val client = mySqlClient.initalizeClient(vertx)
    // app.route().handler(BodyHandler.create)
    app
      .get("/")
      .handler(ctx => {
        // return
        println("inside")

        // println(ctx.user())val authHeader = ctx.request.getHeader("Authorization")
        // Checking for JWT

        // Trying out the Auth handler
        // println(ctx.user().principal())

        val authHeader = ctx.request.getHeader("Authorization")

        val isAuthenticated = authHeader != null
        println(isAuthenticated)
        // println(authHeader)
        if (authHeader.startsWith("Bearer ")) {
          val token = authHeader.substring(7, authHeader.length);

          val authorise = for {
            user <- authenticationService.authenticate(token, ctx)
          } yield user.principal()

          val sub = authorise
            .map(_.getString("sub"))
            .getOrElse({
              return ctx.fail(401)
            })
          ctx.json(sub)
        }

        ctx.json(JsonObject("Status" -> "OK"))
      })

    /*
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
     */

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
