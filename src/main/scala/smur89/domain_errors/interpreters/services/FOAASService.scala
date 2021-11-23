package smur89.domain_errors.interpreters.services

import java.io.IOException

import org.http4s.client.{Client, UnexpectedStatus}
import smur89.domain_errors.Configuration.AppConfig
import smur89.domain_errors.algebras.services.{FOAASAlgebra, FOAASResponse}
import smur89.domain_errors.main.errors.{ClientError, DecodingError, StatusCodeError, UnknownError}

import cats.effect.Sync
import cats.mtl.Raise

import cats.syntax.applicativeError._
import cats.mtl.syntax.raise._

object FOAASService {

  def apply[F[_] : Sync : Raise[*[_], ClientError]](config: AppConfig, client: Client[F]): FOAASAlgebra[F] =
    new FOAASAlgebra[F] {

      override def call(relativePath: String): F[FOAASResponse] =
        client.expect[FOAASResponse](config.externalApi.uri.withPath(relativePath)).handleErrorWith {
          case e: IOException      =>
            DecodingError(e.getMessage).raise
          case e: UnexpectedStatus =>
            StatusCodeError(e.status.code).raise
          case e: Throwable        =>
            UnknownError(e.getMessage).raise
        }

    }

}
