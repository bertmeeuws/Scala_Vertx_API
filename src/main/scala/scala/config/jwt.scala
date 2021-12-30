package scala.config

import io.vertx.core.Vertx
import io.vertx.scala.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.core.buffer.Buffer

object jwt {
  val secret = "put-your-jwt-signing-secret-key-here-it-should-be-strong"
  val pubSecKeyOptions =
    PubSecKeyOptions(
      algorithm = "HS256",
      buffer = Buffer.buffer(scala.config.jwt.secret)
    )
  val vertx    = Vertx.vertx()
  val config   = JWTAuthOptions(pubSecKeys = List(pubSecKeyOptions))
  val provider = JWTAuth.create(vertx, config)

}
