package scala.db
import io.vertx.mysqlclient.MySQLPool
import io.vertx.scala.sqlclient.PoolOptions
import io.vertx.scala.mysqlclient.MySQLConnectOptions
import io.vertx.core.Vertx

object mySqlClient:
  def initalizeClient(vertx: Vertx): MySQLPool = {
    val connectOptions =
      MySQLConnectOptions(port = 3306, host = "localhost", database = "perfume", user = "root", password = "root")

    val poolOptions = PoolOptions(5)

    val client: MySQLPool = MySQLPool.pool(vertx, connectOptions, poolOptions);
    client
  }
