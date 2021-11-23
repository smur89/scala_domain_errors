package smur89.domain_errors.algebras.services

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

import cats.tagless.{Derive, FunctorK}
import cats.effect.Sync

trait FOAASAlgebra[F[_]] {
  def call(relativePath: String): F[FOAASResponse]
}

object FOAASAlgebra {
  implicit val functorK: FunctorK[FOAASAlgebra] = Derive.functorK
}

final case class FOAASResponse(message: String, subtitle: String)

object FOAASResponse {
  implicit val functorK: FunctorK[FOAASAlgebra] = Derive.functorK

  implicit val codec: Codec[FOAASResponse] = deriveCodec
  implicit def entityDecoder[F[_] : Sync]: EntityDecoder[F, FOAASResponse] = jsonOf[F, FOAASResponse]
  implicit def entityEncoder[F[_]]: EntityEncoder[F, FOAASResponse] = jsonEncoderOf[F, FOAASResponse]
}
