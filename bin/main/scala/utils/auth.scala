package scala.utils

import io.vertx.ext.web.RoutingContext
import java.time.LocalDateTime
import java.time.ZoneOffset

object auth {

  def getJwtExpiryUnixEpoch(minutes: Int = 30): Long =
    val now            = LocalDateTime.now
    val minutesFromNow = now.plusMinutes(minutes)
    minutesFromNow.toEpochSecond(ZoneOffset.UTC)

  def retrieveTokenFromHeader(ctx: RoutingContext): String | Unit = {
    val req        = ctx.request();
    val authHeader = req.getHeader("Authorization")

    if (authHeader.startsWith("Bearer ")) {
      val token = authHeader.substring(7, authHeader.length)
      return token
    }
    ctx.fail(401)
  }
}
