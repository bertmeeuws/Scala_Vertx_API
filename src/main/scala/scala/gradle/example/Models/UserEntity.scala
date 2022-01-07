package scala.gradle.example.Models

import java.util.Date
import org.joda.time.DateTime
import io.vertx.scala.core.JsonObject

case class UserEntity(
    username: String,
    password: Option[String] = None,
    name: String,
    surname: String,
    createdAt: Option[DateTime] = None
)
