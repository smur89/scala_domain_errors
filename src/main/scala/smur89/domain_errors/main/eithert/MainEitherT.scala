package smur89.domain_errors.main.eithert

import java.util.concurrent.Executors

import doobie.hikari.HikariTransactor
import io.odin.Level
import io.odin.formatter.Formatter
import io.odin.loggers.ConsoleLogger
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
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
import smur89.domain_errors.main._
import smur89.domain_errors.main.errors._
import smur89.domain_errors.{Configuration, ErrorContextMiddleware}

import cats.data.Kleisli
import cats.tagless.implicits._
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.mtl.Handle.handleForApplicativeError

import cats.syntax.semigroupk._

import scala.concurrent.ExecutionContext

object MainEitherT extends IOApp {
  val HttpPort = 3000
  val HttpHost = "0.0.0.0"

  override def run(args: List[String]): IO[ExitCode] = {

    val config: AppConfig = Configuration()

    val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

    val logger = ConsoleLogger[Run](Formatter.colorful, Level.Info)
    val handledLogger = logger.mapK(handlingS)

    val transactor: HikariTransactor[HandledDatabase] = Database.transactor(config, ec)
    val repo: FOAASRepositoryAlgebra[HandledDatabase] = FOAASRepository(transactor)

    val healthCheck: HealthCheckAlgebra[HandledService] = HealthCheckService[HandledService]

    val client: Resource[Run, Client[Run]] = BlazeClientBuilder[Run](ec).resource

    client.use { client =>
      val handledClient: Client[HandledClient] = client.translate(handlingC)(folding(e1 => UnhandledClientException(e1.message): Throwable))
      val foaas: FOAASAlgebra[HandledService] = FOAASService(config, handledClient).mapK(translating(liftClientToService))
      val comboProgram: DomainComboAlgebra[HandledService] = DomainComboProgram(
        handledLogger,
        foaas,
        repo.mapK(translating(liftDatabaseToService))
      )

      val httpApp: Kleisli[HandledService, Request[HandledService], Response[HandledService]] =
        ErrorContextMiddleware
          .httpRoutes[HandledService, ServiceError](serviceToResponse)
          .apply(
            RequestId(
              HealthCheckRoute[HandledService](handledLogger, healthCheck).impl <+>
                DomainComboRoute(handledLogger, comboProgram).impl
            )
          )
          .orNotFound
      val serverBuilder: BlazeServerBuilder[Run] = BlazeServerBuilder[Run](executionContext)
        .bindHttp(HttpPort, HttpHost)
        .withHttpApp(httpApp.translate(folding(e1 => UnhandledServiceException(e1.message): Throwable))(handlingS))

      Database.migrate[Run](config, logger) *>
        logger.info("Starting Http Server...") *>
        serverBuilder.resource.use(_ => logger.info("Http Server Started") *> IO.never.as(ExitCode.Success))
    }
  }

}
