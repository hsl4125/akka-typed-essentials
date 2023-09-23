package part1recap

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object ThreadModelLimitations {

  // Daniel's rants
  // DR #1 OO encapsulation is only valid in the SINGLE-THREADED MODEL

  private class BankAccount(private var amount: Int) {
    override def toString = s"$amount"

    def withDraw(money: Int): Unit = synchronized {
      this.amount -= money
    }

    def deposit(money: Int): Unit = synchronized {
      this.amount += money
    }

    def getAmount: Int = this.amount
  }

  private val account = new BankAccount(2000)
  private val depositThreads: IndexedSeq[Thread] = (1 to 1000).map(_ => new Thread(() => account.deposit(1)))
  private val withDrawThreads: IndexedSeq[Thread] = (1 to 1000).map(_ => new Thread(() => account.withDraw(1)))

  def demoRace(): Unit = {
    (depositThreads ++ withDrawThreads).foreach(_.start())
    println(account.getAmount)
  }

  /*
   - we don't know when the threads are finished
   - race conditions

   solution: synchronization
   other problems:
    - dead locks
    - live locks
  */

  // DR #2 - delegating a task to a thread

  private var task: Runnable = null

  private val runningThread: Thread = new Thread(() => {
    while (true) {
      while (task == null) {
        runningThread.synchronized {
          println("[background] waiting for a task")
          runningThread.wait()
        }
      }

      task.synchronized {
        println("[background] I have a task!")
        task.run()
        task = null
      }
    }
  })

  private def delegateToBackgroundThread(r: Runnable): Unit = {
    if (task == null) {
      task = r
      runningThread.synchronized {
        runningThread.notify()
      }
    }
  }

  def demoBackgroundDelegation(): Unit = {
    runningThread.start()
    Thread.sleep(1000)
    delegateToBackgroundThread(() => println("I'm running from other thread"))
    Thread.sleep(1000)
    delegateToBackgroundThread(() => println("This should in background again"))
  }

  // DR #3: tracing and dealing with errors is a PINT in multithreaded/distributed apps
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  // sum 1M numbers in between 10 threads
  private val futures = (0 to 9)
    .map(i => BigInt(100000 * i) until BigInt(100000 * (i+1))) // 0 - 99999, 100000 - 199999, and so on
    .map(range => Future {
      // bug
      if (range.contains(BigInt(546732))) throw new Exception("invalid number")
      range.sum
    })

  private val sumFuture = Future.reduceLeft(futures)(_ + _)

  def main(args: Array[String]): Unit = {
    sumFuture.onComplete(println)
  }
}
