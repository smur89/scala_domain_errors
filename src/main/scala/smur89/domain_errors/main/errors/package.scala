package smur89.domain_errors.main

import org.http4s.Response
import org.http4s.Status.{BadRequest, InternalServerError, PayloadTooLarge, ServiceUnavailable}

import cats.Applicative

import cats.syntax.applicative._

package object errors {

  def liftClientToService(clientError: ClientError): ServiceError =
    clientError match {
      case DecodingError(msg)     =>
        ExternalCallGenericError(msg)
      case e @ StatusCodeError(_) =>
        ExternalCallGenericError(e.message)
      case UnknownError(msg)      =>
        ExternalCallGenericError(msg)
    }

  def liftDatabaseToService(clientError: DatabaseError): ServiceError =
    clientError match {
      case TooLongError(msg)         =>
        InputTooLargeError(msg)
      case DatabaseGenericError(msg) =>
        DbError(msg)
    }

  def serviceToResponse[F[_] : Applicative](serviceError: ServiceError): F[Response[F]] =
    serviceError match {
      case NameFormatError(_ @_*)      =>
        Response.apply[F](BadRequest).pure[F]
      case InputTooLargeError(_)       =>
        Response.apply[F](PayloadTooLarge).pure[F]
      case ExternalCallGenericError(_) =>
        Response.apply[F](ServiceUnavailable).pure[F]
      case DbError(_)                  =>
        Response.apply[F](InternalServerError).pure[F]
    }

}
