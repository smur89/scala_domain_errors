package smur89.domain_errors.algebras.routes

import org.http4s.HttpRoutes

trait RoutesAlgebra[F[_]] {
  def impl: HttpRoutes[F]
}
