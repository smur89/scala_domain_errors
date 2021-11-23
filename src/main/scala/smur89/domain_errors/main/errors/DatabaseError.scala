package smur89.domain_errors.main.errors

final case class UnhandledDatabaseException(cause: String) extends Exception(cause)

sealed abstract class DatabaseError(val message: String)

final case class TooLongError(msg: String) extends DatabaseError(s"Provided input is too long. $msg")
final case class DatabaseGenericError(msg: String) extends DatabaseError(s"Database failure. $msg")
