package smur89.playground.monadtransformers

import cats.arrow.FunctionK
import cats.data.EitherT
import cats.tagless.FunctorK.ops.toAllFunctorKOps
import cats.tagless._
import cats.{~>, Applicative}
import cats.effect.{ExitCode, IO, IOApp}
import cats.mtl.Raise

import cats.syntax.applicative._
import cats.syntax.traverse._
import cats.mtl.syntax.raise._

case class User(name: String)

sealed abstract class MyAError(msg: String) extends Throwable(msg)
final case class AError(msg: String) extends MyAError(msg)

sealed abstract class MyBError(msg: String) extends Throwable(msg)
final case class BError(msg: String) extends MyBError(msg)

trait MyAService[F[_]] {
  def doA(user: User): F[Boolean]
}

trait MyBService[F[_]] {
  def doB(user: User): F[Boolean]
}

object MyAService {
  implicit val functorK: FunctorK[MyAService] = Derive.functorK
}

object DomainErrors extends IOApp {

  def getServiceA[F[_] : Applicative : Raise[*[_], MyAError]]: MyAService[F] =
    new MyAService[F] {

      override def doA(user: User): F[Boolean] =
        user.name match {
          case "Shane" =>
            true.pure[F]
          case "JohnA" =>
            AError("Oops A").raise[F, Boolean]
          case _       =>
            false.pure[F]
        }

    }

  def getServiceB[F[_] : Applicative : Raise[*[_], MyBError]]: MyBService[F] =
    new MyBService[F] {

      override def doB(user: User): F[Boolean] =
        user.name match {
          case "Shane" =>
            true.pure[F]
          case "JohnB" =>
            BError("Oops B").raise[F, Boolean]
          case _       =>
            false.pure[F]
        }

    }

  def functionKAtoB(user: User, colour: String): IO[ExitCode] = {
    type F[A] = EitherT[IO, MyAError, A] // We want a type with just one "hole", so we fix the F and E types with an alias
    type G[A] = EitherT[IO, MyBError, A]

    val aToB: FunctionK[F, G] = λ[F ~> G](_.leftMap(aError => BError(aError.getMessage)))
//    new FunctionK[F, G] {
//      override def apply[A](fa: F[A]): G[A] = fa.leftMap(aError => BError(aError.getMessage))
//    }

    val serviceA: MyAService[G] = getServiceA[F].mapK(aToB)
    val serviceB: MyBService[G] = getServiceB[G]

    (for {
      _ <- EitherT.liftF(IO(println(colour + s"Running for ${user.name}")))
      _ <- serviceA.doA(user).leftMap(error => println(colour + s"Error: $error"))
      _ <- serviceB.doB(user).leftMap(error => println(colour + s"Error: $error"))
    } yield ()).value.as(ExitCode.Success)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val programs = List(
      functionKAtoB(User("Shane"), Console.GREEN),
      functionKAtoB(User("JohnA"), Console.RED),
      functionKAtoB(User("JohnB"), Console.YELLOW)
    )
    programs.sequence.map(_.headOption.getOrElse(ExitCode.Error))
  }

}
