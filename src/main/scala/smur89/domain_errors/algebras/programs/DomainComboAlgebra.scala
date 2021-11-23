package smur89.domain_errors.algebras.programs

import smur89.domain_errors.algebras.services.FOAASResponse

import cats.tagless.{Derive, FunctorK}

trait DomainComboAlgebra[F[_]] {
  def saveShort(name: String, author: String): F[String]
  def saveLong(name: String, author: String): F[String]
  def getAll: F[Seq[FOAASResponse]]
}

object DomainComboAlgebra {
  implicit val functorK: FunctorK[DomainComboAlgebra] = Derive.functorK
}
