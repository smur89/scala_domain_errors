package smur89.domain_errors

import org.http4s.server.Middleware
import org.http4s.{HttpRoutes, Request, Response}

import cats.Functor
import cats.data.{Kleisli, OptionT}
import cats.mtl.Handle

import cats.syntax.functor._
import cats.mtl.syntax.handle._

object ErrorContextMiddleware {

  def httpRoutes[F[_] : Handle[*[_], E] : Functor, E](
    fe: E => F[Response[F]]
  ): Middleware[OptionT[F, *], Request[F], Response[F], Request[F], Response[F]] = { routes: HttpRoutes[F] =>
    Kleisli { req: Request[F] => OptionT(routes(req).value.handleWith(fe(_: E).map(Option.apply))) }
  }

}
