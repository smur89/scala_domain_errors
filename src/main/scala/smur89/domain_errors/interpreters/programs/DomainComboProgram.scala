package smur89.domain_errors.interpreters.programs

import io.odin.Logger
import smur89.domain_errors.algebras.persistence.FOAASRepositoryAlgebra
import smur89.domain_errors.algebras.programs.DomainComboAlgebra
import smur89.domain_errors.algebras.services.{FOAASAlgebra, FOAASResponse}
import smur89.domain_errors.main.errors.{NameFormatError, ServiceError}

import cats.Monad
import cats.data.ValidatedNel
import cats.implicits.catsSyntaxSemigroup
import cats.mtl.Raise

import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.validated._
import cats.mtl.syntax.raise._

class DomainComboProgram[F[_] : Monad : Raise[*[_], ServiceError]](
  logger: Logger[F],
  foaas: FOAASAlgebra[F],
  repo: FOAASRepositoryAlgebra[F]
) extends DomainComboAlgebra[F] {

  def saveShort(name: String, author: String): F[String] =
    for {
      _ <- logger.info(s"Running for $name, $author")
      _ <- (validateName(name) |+| validateName(author))
             .leftMap(names => names.foldLeft(NameFormatError(""))(_ |+| _))
             .fold(_.raise, identity(_).pure[F])
      response <- foaas.call(s"/cool/$author")
      _ <- logger.info(s"Saving message '${response.message} ${response.subtitle}' for $name, $author")
      _ <- repo.save(response)
      message = s"$response"
    } yield message

  def saveLong(name: String, author: String): F[String] =
    for {
      _ <- logger.info(s"Running for $name, $author")
      _ <- (validateName(name) |+| validateName(author))
             .leftMap(names => names.foldLeft(NameFormatError(""))(_ |+| _))
             .fold(_.raise, identity(_).pure[F])
      response <- foaas.call(s"/holygrail/$author")
      _ <- logger.info(s"Saving message '${response.message} ${response.subtitle}' for $name, $author")
      _ <- repo.save(response)
      message = s"$response"
    } yield message

  def getAll: F[Seq[FOAASResponse]] =
    for {
      _ <- logger.info(s"Retrieving messages...")
      messages <- repo.getAll
      _ <- logger.info(s"Retrieved ${messages.size} messages")
    } yield messages

  private def validateName(author: String): ValidatedNel[NameFormatError, String] =
    if (author.headOption.exists(_.isUpper)) {
      author.validNel
    } else {
      NameFormatError(author).invalidNel
    }

}

object DomainComboProgram {

  def apply[F[_] : Monad : Raise[*[_], ServiceError]](
    logger: Logger[F],
    foaas: FOAASAlgebra[F],
    repo: FOAASRepositoryAlgebra[F]
  ): DomainComboAlgebra[F] = new DomainComboProgram[F](logger, foaas, repo)

}
