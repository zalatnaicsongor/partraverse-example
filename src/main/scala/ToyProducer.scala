import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

import java.util
import java.util.concurrent.LinkedBlockingQueue

class ToyProducer extends AutoCloseable {
  @volatile private var interrupted = false
  private val queue = new LinkedBlockingQueue[QueueElement](3)

  def publish(record: String): ListenableFuture[String] = {
    val future = SettableFuture.create[String]
    System.out.println(s"Publishing $record @ ${Thread.currentThread().getName}")
    queue.put(QueueElement(record, future))
    future
  }

  def startProcessing(): Unit = {
    System.out.println("starting to process the queue... @ " + Thread.currentThread().getName)
    while !interrupted do {
      try {
        Thread.sleep(100)
        val collection = util.ArrayList[QueueElement]()
        queue.drainTo(collection)
        collection.stream().forEach { elem =>
          System.out.println(s"got ${elem.record} from the queue @ ${Thread.currentThread().getName}")
          elem.response.set("processed: " + elem.record + " @ " + Thread.currentThread().getName)
          ()
        }
      } catch
        case _: InterruptedException => {
          System.out.println("interrupted @ " + Thread.currentThread().getName)
          this.interrupted = true
        }
    }
  }

  override def close(): Unit = interrupted = true
}
case class QueueElement(record: String, response: SettableFuture[String])
