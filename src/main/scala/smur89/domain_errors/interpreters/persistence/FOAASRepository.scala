package smur89.domain_errors.interpreters.persistence

import doobie.implicits._
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import org.postgresql.util.PSQLException
import smur89.domain_errors.algebras.persistence.FOAASRepositoryAlgebra
import smur89.domain_errors.algebras.services.FOAASResponse
import smur89.domain_errors.main.errors.{DatabaseError, DatabaseGenericError, TooLongError}

import cats.effect.BracketThrow
import cats.mtl.Raise

import cats.syntax.applicativeError._
import cats.syntax.functor._
import cats.mtl.syntax.raise._

object FOAASRepository {

  def apply[F[_] : BracketThrow : Raise[*[_], DatabaseError]](tx: Transactor[F]): FOAASRepositoryAlgebra[F] =
    new FOAASRepositoryAlgebra[F] {
      override def save(resp: FOAASResponse): F[Unit] = sql.save(resp).run.transact(tx).void.handleErrorWith(errorHandler)

      override def getAll: F[Seq[FOAASResponse]] = sql.getAll.to[Seq].transact(tx)
    }

  def errorHandler[F[_] : Raise[*[_], DatabaseError], A]: Throwable => F[A] = {
    case e: PSQLException if e.getSQLState == "22001" =>
      TooLongError(e.getServerErrorMessage.getMessage).raise
    case e                                            =>
      DatabaseGenericError(e.getMessage).raise
  }

  private object sql {
    def save(resp: FOAASResponse): doobie.Update0 =
      sql"""INSERT INTO foaas (message, subtitle) VALUES (${resp.message}, ${resp.subtitle})""".stripMargin.update

    def getAll: doobie.Query0[FOAASResponse] =
      sql"""SELECT message, subtitle FROM foaas""".queryWithLogHandler[FOAASResponse](LogHandler.jdkLogHandler)
  }

}
