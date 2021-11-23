package smur89.domain_errors

import org.http4s.Uri
import pureconfig.{ConfigReader, ConfigSource, ConvertHelpers}

object Configuration {

  final case class ExternalApi(uri: Uri)

  final case class DbConfig(
    url: String,
    user: String,
    password: String,
    schema: String,
    driver: String,
    minIdle: Int,
    maxPoolSize: Int
  )

  final case class AppConfig(externalApi: ExternalApi, dbConfig: DbConfig)

  def apply(): AppConfig = {
    import pureconfig.generic.auto._

    implicit val uriReader: ConfigReader[Uri] = ConfigReader.fromString(ConvertHelpers.catchReadError(Uri.unsafeFromString))

    ConfigSource.default.loadOrThrow[AppConfig]
  }

}
