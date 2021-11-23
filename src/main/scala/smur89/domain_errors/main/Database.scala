package smur89.domain_errors.main

import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.HikariTransactor
import io.odin.Logger
import org.flywaydb.core.Flyway
import smur89.domain_errors.Configuration.AppConfig

import cats.{Applicative, Defer}
import cats.effect.{Async, Blocker, ContextShift}

import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.functor._

import scala.concurrent.ExecutionContext

object Database {

  def migrate[F[_] : Applicative](config: AppConfig, logger: Logger[F])(implicit F: Defer[F]): F[Unit] =
    logger.info("Running migrations...") *> F
      .defer(
        Flyway
          .configure()
          .schemas(config.dbConfig.schema)
          .defaultSchema(config.dbConfig.schema)
          .dataSource(s"jdbc:${config.dbConfig.url}", config.dbConfig.user, config.dbConfig.password)
          .locations("db/migration")
          .load()
          .pure[F]
      )
      .map { flyway => flyway.migrate(); flyway.validate() }

  def transactor[F[_] : Async : ContextShift](config: AppConfig, ec: ExecutionContext): HikariTransactor[F] = {
    val blocker: Blocker = Blocker.liftExecutionContext(ec)
    val source: HikariDataSource = {
      val s = new HikariDataSource()
      s.setDriverClassName(config.dbConfig.driver)
      s.setJdbcUrl(s"jdbc:${config.dbConfig.url}")
      s.setUsername(config.dbConfig.user)
      s.setPassword(config.dbConfig.password)
      s.setMinimumIdle(config.dbConfig.minIdle)
      s.setMaximumPoolSize(config.dbConfig.maxPoolSize)
      s.setSchema(config.dbConfig.schema)
      s
    }
    HikariTransactor[F](source, ec, blocker)
  }

}
