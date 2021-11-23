package smur89.domain_errors.interpreters.routes

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import io.odin.Logger
import org.http4s.Method.POST
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.{EntityDecoder, HttpRoutes, Response, Status}
import smur89.domain_errors.algebras.programs.DomainComboAlgebra
import smur89.domain_errors.algebras.routes.RoutesAlgebra

import cats.effect.Sync

import cats.syntax.flatMap._
import cats.syntax.functor._

final case class DomainComboRequest(name: String, author: String)

object DomainComboRequest {
  implicit val codec: Codec[DomainComboRequest] = deriveCodec
  implicit def entityDecoder[F[_] : Sync]: EntityDecoder[F, DomainComboRequest] = jsonOf[F, DomainComboRequest]
}

object DomainComboRoute {

  def apply[F[_] : Sync](logger: Logger[F], program: DomainComboAlgebra[F]): RoutesAlgebra[F] =
    new RoutesAlgebra[F] {

      override def impl: HttpRoutes[F] =
        HttpRoutes.of[F] {
          case req @ POST -> Root / "domain" / "short" =>
            for {
              request <- req.as[DomainComboRequest]
              _ <- logger.trace("Running request...")
              resp <- program.saveShort(request.name, request.author)
            } yield Response[F](status = Status.Ok).withEntity(resp)

          case req @ POST -> Root / "domain" / "long" =>
            for {
              request <- req.as[DomainComboRequest]
              _ <- logger.trace("Running request...")
              resp <- program.saveLong(request.name, request.author)
            } yield Response[F](status = Status.Ok).withEntity(resp)

          case GET -> Root / "domain" =>
            for {
              _ <- logger.trace("Getting all saved messages...")
              resp <- program.getAll
            } yield Response[F](status = Status.Ok).withEntity(resp)
        }

    }

}
