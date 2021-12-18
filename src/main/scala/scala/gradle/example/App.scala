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

val PORT = 8000

case class Perfume(id: Int, name: String, description: String)

class HttpVerticle extends ScalaVerticle {
  override def start(promise: Promise[Unit]): Unit = {
    // Create a router
    val app = Router.router(vertx)

    // Sets up JSON parsing
    app.route().handler(BodyHandler.create)

    app
      .get("/hello")
      .handler(ctx => ctx.response.end("world"))

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
