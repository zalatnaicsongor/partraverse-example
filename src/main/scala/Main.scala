import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.parallel.*
import cats.syntax.traverse.*

import java.util.concurrent.TimeUnit
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val queue = Queue()

    // emulate a background processing thread
    System.out.println(Future {
      blocking {
        queue.tailQueue()
      }
    })
    for {
      _ <- (1 to 100).toList.traverse(elem => queue.offer("traverse " + elem))
      _ <- IO.sleep(Duration.apply(2, TimeUnit.SECONDS))
      _ <- (1 to 100).toList.parTraverse(elem => queue.offer("parTraverse " + elem))
      _ <- IO.sleep(Duration.apply(2, TimeUnit.SECONDS))
      _ <- (1 to 100).toList.parUnorderedTraverse(elem => queue.offer("parUnorderedTraverse " + elem))
      _ <- IO.sleep(Duration.apply(2, TimeUnit.SECONDS))
    } yield ExitCode.Success

  }
}
