package smur89.domain_errors.main

import cats.{Applicative, ApplicativeError, Functor}
import cats.effect.IO
import cats.mtl.{Handle, Raise}

package object effectio {

  implicit def raiseIo[E]: Raise[Run, E] =
    new Raise[Run, E] {
      override def functor: Functor[Run] = implicitly[Functor[Run]]

      override def raise[E2 <: E, A](e: E2): Run[A] =
        implicitly[ApplicativeError[Run, Throwable]].raiseError(new RuntimeException(e.toString))
    }

  implicit def handleIO[E]: Handle[Run, E] =
    new Handle[Run, E] {
      override def applicative: Applicative[Run] = implicitly[Applicative[Run]]

      override def handleWith[A](fa: Run[A])(f: E => Run[A]): Run[A] =
        fa.handleErrorWith(_ => IO.raiseError(new Exception("We don't know what error this is")))

      override def raise[E2 <: E, A](e: E2): Run[A] = raiseIo.raise(e)
    }

}
