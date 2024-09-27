import cats.effect.kernel.Deferred
import cats.effect.std.Dispatcher
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.syntax.traverse.*
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures

import java.util.concurrent.Executors

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val producerResource = Resource.fromAutoCloseable(IO.pure(ToyProducer()))
    val threadPoolResource = Resource.fromAutoCloseable(IO.pure(Executors.newCachedThreadPool()))
    val dispatcherResource = Dispatcher.sequential[IO]

    producerResource
      .use { producer =>
        threadPoolResource.use { threadPool =>
          System.out.println(threadPool.submit(new Runnable {
            override def run(): Unit = producer.startProcessing()
          }))
          dispatcherResource.use { dispatcher =>
            IO.println(s"Main - Starting execution @ ${Thread.currentThread().getName}").flatMap { _ =>
              (1 to 10).toList
                .traverse { elem =>
                  produceDeferred(producer, elem.toString, dispatcher)
                }
                .flatMap(deferreds => {
                  deferreds.traverse(deferred => deferred.get)
                })
                .map(resolvedList => {
                  println(s"Main - received result: $resolvedList @ ${Thread.currentThread().getName}")
                  System.out.println(threadPool.shutdownNow())
                  resolvedList
                })
            }
          }
        }
      }
      .map(_ => {
        ExitCode.Success
      })
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
