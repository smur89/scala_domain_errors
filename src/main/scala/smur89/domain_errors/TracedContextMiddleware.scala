package smur89.domain_errors

import java.util.UUID

import org.http4s.server.Middleware
import org.http4s.server.middleware.RequestId
import org.http4s.util.CaseInsensitiveString
import org.http4s.{ContextRequest, Header, Request, Response}
import smur89.domain_errors.interpreters.logging.TracedContext

import cats.data.Kleisli
import cats.{Applicative, Functor, Monad}

import cats.syntax.applicative._
import cats.syntax.eq._
import cats.syntax.functor._

object TracedContextMiddleware {

  def httpApp[F[_] : Monad]: Middleware[F, ContextRequest[F, TracedContext], Response[F], Request[F], Response[F]] = {
    app: Kleisli[F, ContextRequest[F, TracedContext], Response[F]] => extractTracedContext andThen propagateTraceContext(app)
  }

  private def extractTracedContext[F[_] : Applicative]: Kleisli[F, Request[F], ContextRequest[F, TracedContext]] =
    Kleisli { request: Request[F] =>
      val correlationId = request.attributes.lookup(RequestId.requestIdAttrKey).fold(UUID.randomUUID.toString)(identity)

      ContextRequest[F, TracedContext](
        TracedContext(correlationId),
        request.putHeaders(Header.Raw(CaseInsensitiveString("X-Correlation-ID"), correlationId))
      ).pure
    }

  private def propagateTraceContext[F[_] : Functor](
    routes: Kleisli[F, ContextRequest[F, TracedContext], Response[F]]
  ): Kleisli[F, ContextRequest[F, TracedContext], Response[F]] =
    Kleisli { case a @ ContextRequest(_, request: Request[F]) =>
      routes(a).map(_.putHeaders(request.headers.filter(_.name === CaseInsensitiveString("X-Correlation-ID")).toList: _*))
    }

}
