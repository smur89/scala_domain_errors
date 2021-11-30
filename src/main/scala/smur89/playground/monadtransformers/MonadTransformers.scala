package smur89.playground.monadtransformers

import cats.Applicative
import cats.data.{EitherT, ReaderT}
import cats.effect.{ExitCode, IO, IOApp}
import cats.mtl.Raise

import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import cats.mtl.syntax.raise._

import scala.util.Random

object Playground {

  case class User(name: String)

  trait UserService {
    def getUser(id: Int): IO[Either[String, Option[User]]]
  }

  trait AddressService {
    def getAddress(user: User): IO[Option[String]]
  }

  trait UserServiceMtl[F[_]] {
    def getUser(id: Int): F[Option[User]]
  }

  trait AddressService2[F[_]] {
    def getAddress(user: User): F[Option[String]]
  }

}

object MonadTransformers extends IOApp {
  import Playground._

  val userService: UserService =
    new UserService {

      override def getUser(id: Int): IO[Either[String, Option[User]]] =
        if (id == 1) {
          IO(User("Shane").some.asRight)
        } else
          IO("No such user".asLeft)

    }

  val addressService: AddressService =
    new AddressService {
      override def getAddress(user: User): IO[Option[String]] = IO("123 My Street".some)
    }

  private val plainImplForComp: IO[ExitCode] = {
    IO(println(Console.RED + "PlainImpl For Comprehension")) *>
      (for { // Outer Monad is IO
        nameEither <- userService.getUser(1) // IO[Either[String, Option[User]]
//         name <- nameEither // does not compose (IO cannot compose with Either)
        address <- // IO[Option[String]]
          nameEither match {
            case Right(userOpt) =>
              userOpt match {
                case Some(user) =>
                  IO(println(s"Name: $user")) *> addressService.getAddress(user)
                case None       =>
                  IO(None)
              }
            case Left(_)        =>
              IO(None)
          }
        _ <- IO(println(s"Address: $address")) // IO[Unit]
      } yield address).as(ExitCode.Success)
  }

  private val monadTransformerEitherImpl: IO[ExitCode] = {
    IO(println(Console.GREEN + "Monad Transformer EitherT")) *>
      (for { // Outer Monad is EitherT
        nameOption <- EitherT(userService.getUser(1)) // EitherT[IO, String, Option[User]]
        name <- nameOption.liftTo[EitherT[IO, String, *]]("Unknown User") // EitherT[IO, String, User]
        _ <- EitherT(IO(println(s"Name: $name").asRight[String])) // EitherT[IO, String, Unit]
        address <- EitherT.fromOptionF(addressService.getAddress(name), "No Address") // EitherT[IO, String, String]
        _ <- EitherT(IO(println(s"Address: $address").asRight[String])) // EitherT[IO, String, Unit]
      } yield address).value.as(ExitCode.Success)
  }

  private val monadTransformerReaderTImpl: IO[ExitCode] = {
    IO(println(Console.YELLOW + "Monad Transformer ReaderT")) *>
      (for { // Outer Monad is EitherT
        nameOption <- ReaderT.liftF[EitherT[IO, String, *], String, Option[User]](
                        EitherT(userService.getUser(1))
                      ) // ReaderT[EitherT[IO, String, Option[User]], String, Option[User]]
        name <- nameOption.liftTo[ReaderT[EitherT[IO, String, *], String, *]](
                  "Unknown User"
                ) // ReaderT[EitherT[IO, String, User], String, User]
        _ <- ReaderT.liftF[EitherT[IO, String, *], String, Unit](
               EitherT(IO(println(s"Name: $name").asRight[String]))
             ) // ReaderT[EitherT[IO, String, Unit], String, Unit]
        address <- ReaderT.liftF[EitherT[IO, String, *], String, String](
                     EitherT.fromOptionF(addressService.getAddress(name), "No Address")
                   ) // ReaderT[EitherT[IO, String, String], String, String]
        _ <- ReaderT.liftF[EitherT[IO, String, *], String, Unit](
               EitherT(IO(println(s"Address: $address").asRight[String]))
             ) // ReaderT[EitherT[IO, String, Unit], String, Unit]
      } yield address).run("Some String").value.as(ExitCode.Success)
  }

  def userServiceMtl[F[_] : Raise[*[_], String] : Applicative]: UserServiceMtl[F] =
    new UserServiceMtl[F] {

      override def getUser(id: Int): F[Option[User]] =
        if (id == 1) {
          User("Shane").some.pure[F]
        } else
          "No such user".raise

    }

  def addressServiceMtl[F[_] : Applicative]: AddressService2[F] =
    new AddressService2[F] {
      override def getAddress(user: User): F[Option[String]] = "123 My Street".some.pure[F]
    }

  private val mtlEitherTImpl = {
    type F[A] = EitherT[IO, String, A]
    IO(println(Console.BLUE + "MTL EitherT")) *>
      (for { // Outer Monad is F
        nameOpt <- userServiceMtl[F].getUser(1) // F[Option[User]]
        name <- nameOpt.liftTo[F]("Unknown User") // F[User]
        _ <- println(s"Name: $name").pure[F] // F[Unit]
        address <- addressServiceMtl[F].getAddress(name) // F[Option[String]]
        _ <- println(s"Address: $address").pure[F] // F[Unit]
      } yield address).value.as(ExitCode.Success)
  }

  private val mtlReaderTImpl = {
    import cats.mtl.Handle.handleForApplicativeError

    type F[A] = ReaderT[EitherT[IO, String, *], String, A]
    IO(println(Console.GREEN + "MTL ReaderT")) *>
      (for { // Outer Monad is F
        nameOpt <- userServiceMtl[F].getUser(1) // F[Option[User]]
        name <- nameOpt.liftTo[F]("Unknown User") // F[User]
        _ <- println(s"Name: $name").pure[F] // F[Unit]
        address <- addressServiceMtl[F].getAddress(name) // F[Option[String]]
        _ <- println(s"Address: $address").pure[F] // F[Unit]
      } yield address).run("Some string").value.as(ExitCode.Success)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val programs = List(plainImplForComp, monadTransformerEitherImpl, monadTransformerReaderTImpl, mtlEitherTImpl, mtlReaderTImpl)
    Random.shuffle(programs).head
  }

}
