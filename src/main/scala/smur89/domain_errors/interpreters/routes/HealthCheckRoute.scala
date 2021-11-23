package smur89.domain_errors.interpreters.routes

import io.odin.Logger
import org.http4s.Method.GET
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io.{->, /}
import org.http4s.{HttpRoutes, Response, Status}
import smur89.domain_errors.algebras.routes.RoutesAlgebra
import smur89.domain_errors.algebras.services.HealthCheckAlgebra

import cats.{Applicative, Defer}

import cats.syntax.applicative._
import cats.syntax.apply._

object HealthCheckRoute {

  def apply[F[_] : Applicative : Defer](logger: Logger[F], health: HealthCheckAlgebra[F]): RoutesAlgebra[F] =
    new RoutesAlgebra[F] {

      override def impl: HttpRoutes[F] =
        HttpRoutes.of[F] { case GET -> Root / "healthz" =>
          logger.info("Checking Health...") *> health.status *> Response[F](Status.NoContent).pure[F]
        }

    }

}
