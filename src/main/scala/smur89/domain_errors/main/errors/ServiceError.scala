package smur89.domain_errors.main.errors

import cats.Semigroup

final case class UnhandledServiceException(cause: String) extends Exception(cause)

sealed abstract class ServiceError(val message: String)

final case class NameFormatError(names: String*)
  extends ServiceError(s"Names ${names.mkString("'", ",", "'")} is not formatted correctly. Hint: Must start with a capital letter.")

object NameFormatError {

  implicit val semigroup =
    new Semigroup[NameFormatError] {
      override def combine(x: NameFormatError, y: NameFormatError): NameFormatError = NameFormatError(x.names ++ y.names: _*)
    }

}

final case class InputTooLargeError(msg: String) extends ServiceError(msg)

final case class ExternalCallGenericError(msg: String) extends ServiceError(s"Failed to make external call. $msg")
final case class DbError(msg: String) extends ServiceError(s"Failed to make external call. $msg")
