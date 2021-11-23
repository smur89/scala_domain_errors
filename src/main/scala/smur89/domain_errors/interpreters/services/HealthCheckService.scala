package smur89.domain_errors.interpreters.services

import smur89.domain_errors.algebras.services.HealthCheckAlgebra

import cats.Applicative

object HealthCheckService {

  def apply[F[_] : Applicative]: HealthCheckAlgebra[F] =
    new HealthCheckAlgebra[F] {
      def status: F[Unit] = Applicative[F].unit
    }

}
