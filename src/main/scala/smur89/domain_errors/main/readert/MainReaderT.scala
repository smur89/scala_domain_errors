package smur89.domain_errors.main.readert

import java.util.concurrent.Executors

import doobie.hikari.HikariTransactor
import io.odin.formatter.Formatter
import io.odin.loggers.ConsoleLogger
import io.odin.syntax._
import io.odin.{Level, Logger}
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware
import org.http4s.server.middleware.RequestId
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
import smur89.domain_errors.{Configuration, ErrorContextMiddleware, TracedContextMiddleware}

import cats.data.{Kleisli, ReaderT}
import cats.tagless.FunctorK.ops.toAllFunctorKOps
import cats.effect.Bracket.catsKleisliBracket
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.mtl.Handle.{handleForApplicativeError, handleKleisli}

import cats.syntax.semigroupk._

import scala.concurrent.ExecutionContext

object MainReaderT extends IOApp {
  val HttpPort = 3000
  val HttpHost = "0.0.0.0"

  override def run(args: List[String]): IO[ExitCode] = {

    val config: AppConfig = Configuration()

    val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

    val logger: Logger[Run] = ConsoleLogger[Run](Formatter.colorful, Level.Info)
    val handledLogger: Logger[HandledDatabase] = logger.mapK(handlingD)
    val tracedLogger: Logger[TracedService] = logger.mapK(liftingS).withContext

    val transactor: HikariTransactor[HandledDatabase] = Database
      .transactor[HandledDatabase](config, ec)
      .mapK(errorLogging("DB Transactor Error", handledLogger))
    val repo: FOAASRepositoryAlgebra[HandledService] = FOAASRepository(transactor).mapK(
      errorLogging("DB Error", handledLogger) andThen translating(liftDatabaseToService)
    )

    val healthCheck: HealthCheckAlgebra[TracedService] = HealthCheckService[TracedService]

    val client: Resource[Run, Client[Run]] = BlazeClientBuilder[Run](ec).resource

    client.use { client =>
      val handledLogger = logger.mapK(handlingC)
      val handledClient: Client[HandledClient] =
        client.translate(handlingC)(
          errorLogging("Client Error", handledLogger) andThen folding(e1 => UnhandledClientException(e1.message): Throwable)
        )
      val foaas: FOAASAlgebra[HandledService] = FOAASService(config, handledClient).mapK(
        errorLogging("Service Error", handledLogger) andThen translating(liftClientToService)
      )
      val comboProgram: DomainComboAlgebra[TracedService] = DomainComboProgram[TracedService](
        tracedLogger,
        foaas.mapK(tracingS),
        repo.mapK(tracingS)
      ).mapK(
        errorLogging("Program Error", tracedLogger)
      )

      val httpApp: Kleisli[TracedService, Request[TracedService], Response[TracedService]] =
        ErrorContextMiddleware
          .httpRoutes[TracedService, ServiceError](serviceToResponse)
          .apply(
            RequestId(
              HealthCheckRoute[TracedService](tracedLogger, healthCheck).impl <+> DomainComboRoute(tracedLogger, comboProgram).impl
            )
          )
          .orNotFound

      val serverBuilder: BlazeServerBuilder[Run] = BlazeServerBuilder[Run](executionContext)
        .bindHttp(HttpPort, HttpHost)
        .withHttpApp(
          middleware.Logger.httpApp[Run](
            logHeaders = true,
            logBody = true,
            logAction = Some(str => logger.info(str))
          )(
            TracedContextMiddleware
              .httpApp[Run]
              .apply(
                ReaderT { case ContextRequest(traceContext, request) =>
                  httpApp
                    .translate[Run](
                      errorLogging("Server Error", tracedLogger) andThen
                        providing(traceContext) andThen
                        folding(e1 => UnhandledServiceException(e1.message): Throwable)
                    )(liftingS)
                    .run(request)
                }
              )
          )
        )

      Database.migrate[Run](config, logger) *>
        logger.info("Starting Http Server...") *>
        serverBuilder.resource.use(_ => logger.info("Http Server Started") *> IO.never.as(ExitCode.Success))
    }
  }

}
