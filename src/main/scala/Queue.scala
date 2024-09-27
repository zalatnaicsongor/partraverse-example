import cats.effect.IO

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

class Queue {
  private val blockingQueue = new LinkedBlockingQueue[String]()

  @volatile var interrupted = false

  // This is supposed to be a wrapper over a third party resource (in this case a queue like KPL))
  def offer(msg: String): IO[Unit] = {
    IO.delay {
      Thread.sleep(10)
      if (blockingQueue.offer(msg, 5, TimeUnit.SECONDS)) {
        System.out.println(s"offered $msg @ " + Thread.currentThread().getName)
      } else {
        throw new RuntimeException(s"FAILED TO OFFER $msg")
      }
    }
  }

  // This lives OUTSIDE cats-effect to better emulate a third party lib.
  def tailQueue(): Unit = {
    System.out.println("draining the queue begins now")
    while !interrupted do {
      try {
        Thread.sleep(15)
        System.out.println(s"draining ${blockingQueue.take()}")
      } catch case _: InterruptedException => interrupted = true
    }
    System.out.println("draining the queue has ended")
  }
}
