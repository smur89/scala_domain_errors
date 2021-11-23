package smur89.domain_errors.algebras.persistence

import smur89.domain_errors.algebras.services.FOAASResponse

import cats.tagless.{Derive, FunctorK}

trait FOAASRepositoryAlgebra[F[_]] {
  def save(response: FOAASResponse): F[Unit]
  def getAll: F[Seq[FOAASResponse]]
}

object FOAASRepositoryAlgebra {
  implicit val functorK: FunctorK[FOAASRepositoryAlgebra] = Derive.functorK
}
