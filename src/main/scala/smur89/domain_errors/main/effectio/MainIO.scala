package smur89.domain_errors.main.effectio

import java.util.concurrent.Executors

import doobie.hikari.HikariTransactor
import io.odin.Level
import io.odin.formatter.Formatter
import io.odin.loggers.ConsoleLogger
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.RequestId
import org.http4s.{Request, Response}
import smur89.domain_errors.Configuration.AppConfig
import smur89.domain_errors.algebras.persistence.FOAASRepositoryAlgebra
import smur89.domain_errors.algebras.programs.DomainComboAlgebra
import smur89.domain_errors.algebras.services.{FOAASAlgebra, HealthCheckAlgebra}
import smur89.domain_errors.interpreters.persistence.FOAASRepository
import smur89.domain_errors.interpreters.programs.DomainComboProgram
import smur89.domain_errors.interpreters.routes.{DomainComboRoute, HealthCheckRoute}
import smur89.domain_errors.interpreters.services.{FOAASService, HealthCheckService}
import smur89.domain_errors.main.errors.{serviceToResponse, ServiceError}
import smur89.domain_errors.main.{Database, Run}
import smur89.domain_errors.{Configuration, ErrorContextMiddleware}

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp, Resource}

import cats.syntax.semigroupk._

import scala.concurrent.ExecutionContext

object MainIO extends IOApp {
  val HttpPort = 3000
  val HttpHost = "0.0.0.0"

  override def run(args: List[String]): IO[ExitCode] = {

    val config: AppConfig = Configuration()

    val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

    val logger = ConsoleLogger[Run](Formatter.colorful, Level.Info)

    val transactor: HikariTransactor[Run] = Database.transactor(config, ec)
    val repo: FOAASRepositoryAlgebra[Run] = FOAASRepository(transactor)

    val healthCheck: HealthCheckAlgebra[Run] = HealthCheckService[Run]

    val client: Resource[Run, Client[Run]] = BlazeClientBuilder[Run](ec).resource

    client.use { client =>
      val foaas: FOAASAlgebra[Run] = FOAASService(config, client)
      val comboProgram: DomainComboAlgebra[Run] = DomainComboProgram(logger, foaas, repo)

      val httpApp: Kleisli[Run, Request[Run], Response[Run]] =
        ErrorContextMiddleware
          .httpRoutes[Run, ServiceError](serviceToResponse)
          .apply(
            RequestId(
              HealthCheckRoute[Run](logger, healthCheck).impl <+>
                DomainComboRoute(logger, comboProgram).impl
            )
          )
          .orNotFound

      val serverBuilder: BlazeServerBuilder[Run] = BlazeServerBuilder[Run](executionContext)
        .bindHttp(HttpPort, HttpHost)
        .withHttpApp(httpApp)

      Database.migrate[Run](config, logger) *>
        logger.info("Starting Http Server...") *>
        serverBuilder.resource.use(_ => logger.info("Http Server Started") *> IO.never).as(ExitCode.Success)
    }
  }

}
