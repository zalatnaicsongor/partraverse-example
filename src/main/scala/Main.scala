import cats.effect.kernel.Deferred
import cats.effect.std.Dispatcher
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.syntax.parallel.*
import cats.syntax.traverse.*
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val producerResource = Resource.fromAutoCloseable(IO.pure(ToyProducer()))
    val dispatcherResource = Dispatcher.sequential[IO]

    producerResource
      .use { producer =>
        dispatcherResource
          .use { dispatcher =>
            IO.both(
              for {
                _ <- IO.println(s"produceDeferred - Starting execution @ ${Thread.currentThread().getName}")
                deferreds <- (1 to 10).toList.traverse { elem =>
                  produceDeferred(producer, "produceDeferred " + elem.toString, dispatcher)
                }
                results <- deferreds.map(_.get).parSequence
                _ <- IO.println(s"produceDeferred - received result: $results @ ${Thread.currentThread().getName}")
              } yield (),
              for {
                _ <- IO.println(s"produceIOIO - Starting execution @ ${Thread.currentThread().getName}")
                ioios <- (1 to 10).toList.traverse { elem =>
                  produceIOIO(producer, "produceIOIO " + elem.toString)
                }
                results <- ioios.parSequence
                _ <- IO.println(s"produceIOIO - received result: $results @ ${Thread.currentThread().getName}")
              } yield ()
            )
          }
      }
      .map(_ => {
        ExitCode.Success
      })
  }

  def produceIOIO(producer: ToyProducer, str: String): IO[IO[String]] = {
    IO.executor.flatMap { executor =>
      IO.blocking {
        val future = producer.publish(str)
        IO.async[String] { cb =>
          IO.delay {
            Futures.addCallback(
              future,
              new FutureCallback[String] {
                override def onSuccess(result: String): Unit = {
                  val _ = cb(Right(result))
                }

                override def onFailure(t: Throwable): Unit = cb(Left(t))
              },
              executor
            )
            Some(IO {
              future.cancel(true)
              ()
            })
          }
        }
      }
    }
  }

  def produceDeferred(producer: ToyProducer, str: String, dispatcher: Dispatcher[IO]): IO[Deferred[IO, String]] = {
    Deferred[IO, String].flatMap { deferred =>
      IO.executor.flatMap { executor =>
        IO.blocking {
          val future = producer.publish(str)
          Futures.addCallback(
            future,
            new FutureCallback[String] {
              override def onSuccess(result: String): Unit = {
                val _ = dispatcher.unsafeRunSync(deferred.complete(result))
              }

              override def onFailure(t: Throwable): Unit = throw t
            },
            executor
          )
          deferred
        }
      }
    }
  }

}
