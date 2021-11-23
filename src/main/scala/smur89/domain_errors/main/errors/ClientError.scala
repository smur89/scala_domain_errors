package smur89.domain_errors.main.errors

final case class UnhandledClientException(cause: String) extends Exception(cause)

sealed abstract class ClientError(val message: String)

final case class StatusCodeError(code: Int) extends ClientError(s"Status Code was $code")
final case class DecodingError(msg: String) extends ClientError(msg)
final case class UnknownError(msg: String) extends ClientError(msg)
