package scala.gradle.example.Models

import java.util.Date
import org.joda.time.DateTime

final case class UserEntity(username: String, password: String, name: String, surname: String, createdAt: DateTime)
