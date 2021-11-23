package smur89.domain_errors.algebras.services

trait HealthCheckAlgebra[F[_]] {
  def status: F[Unit]
}
