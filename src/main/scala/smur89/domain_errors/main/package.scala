package smur89.domain_errors

import io.odin.Logger
import smur89.domain_errors.interpreters.logging.TracedContext
import smur89.domain_errors.main.errors.{ClientError, DatabaseError, ServiceError}

import cats.data.{EitherT, Kleisli, ReaderT}
import cats.{~>, Applicative, FlatMap, Functor, Monad}
import cats.effect.IO
import cats.mtl.{Handle, Raise}

import cats.syntax.apply._

package object main {

  type Run[A] = IO[A]
  type HandledDatabase[A] = EitherT[IO, DatabaseError, A]
  type HandledService[A] = EitherT[IO, ServiceError, A]
  type HandledClient[A] = EitherT[IO, ClientError, A]
  type TracedDatabase[A] = ReaderT[HandledDatabase, TracedContext, A]
  type TracedService[A] = ReaderT[HandledService, TracedContext, A]
  type TracedClient[A] = ReaderT[HandledClient, TracedContext, A]

  val handlingD: Run ~> HandledDatabase = EitherT.liftK[Run, DatabaseError]
  val handlingS: Run ~> HandledService = EitherT.liftK[Run, ServiceError]
  val handlingC: Run ~> HandledClient = EitherT.liftK[Run, ClientError]
  val tracingD: HandledDatabase ~> TracedDatabase = ReaderT.liftK[HandledDatabase, TracedContext]
  val tracingS: HandledService ~> TracedService = ReaderT.liftK[HandledService, TracedContext]
  val tracingC: HandledClient ~> TracedClient = ReaderT.liftK[HandledClient, TracedContext]
  val liftingD: Run ~> TracedDatabase = handlingD andThen tracingD
  val liftingS: Run ~> TracedService = handlingS andThen tracingS
  val liftingC: Run ~> TracedClient = handlingC andThen tracingC

  def translating[F[_] : Functor, E2, E1](f: E1 => E2): EitherT[F, E1, *] ~> EitherT[F, E2, *] =
    λ[EitherT[F, E1, *] ~> EitherT[F, E2, *]](_.leftMap(e1 => f(e1)))

  def folding[F[_] : Monad, E, E1](f: E1 => E)(implicit F: Raise[F, _ >: E]): EitherT[F, E1, *] ~> F =
    λ[EitherT[F, E1, *] ~> F](_.foldF(e1 => F.raise(f(e1)), a => Applicative[F].pure(a)))

  def errorLogging[F[_] : FlatMap, E](message: String, logger: Logger[F])(implicit F: Handle[F, E]): F ~> F =
    λ[F ~> F](F.handleWith(_)(e => logger.error(s"$message: $e") *> F.raise(e)))

  def providing[F[_], A](e: A): Kleisli[F, A, *] ~> F = λ[Kleisli[F, A, *] ~> F](_.run(e))

}
