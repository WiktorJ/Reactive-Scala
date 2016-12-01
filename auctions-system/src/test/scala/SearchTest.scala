
import actors._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import common._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

/**
  * Created by wiktor on 01/12/16.
  */
class SearchTest extends TestKit(ActorSystem(CommonNames.systemName))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {


  val sellerAmount = 3
  val buyerAmount = 15
  val config = ConfigFactory.load()
  var testSystem = ActorSystem("testSystem", config.getConfig(CommonNames.localServiceConfig).withFallback(config))
  var auctions: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]()
  var createAuction = true

  val notifier = testSystem.actorOf(Props[Notifier])

//  val auctionFactory = new PublishFSMPersistAuctionFactory(notifier)
  val auctionFactory = new FSMPersistAuctionFactory()

  val auctionNames: Array[String] = Array("Audi A6 diesel manual",
    "Audi A4 automatic",
    "BMW x6 manual",
    "Casio watch waterproof",
    "Rolex watch",
    "Samsung laptop 8gb ram",
    "Apple laptop 16bg ram",
    "Lenovo notebook 4gb ram")

  val keyWords: Set[String] = auctionNames.foldLeft(Set[String]())((set, name) =>
    set ++ name.split(" "))

//  publishSystem.actorOf(Props[AuctionPublisher], CommonNames.publishServiceName)

  var searchActor = testSystem.actorOf(Props(new MasterSearch(50)), CommonNames.auctionSearchActorName)

  override def afterAll(): Unit = {
    system.terminate
  }

  "Performance test" must {
    "Return results" in {
      val seller: ActorRef = testSystem.actorOf(Props(new Seller(1.toString, auctionFactory, auctionNames, () => 2000 + Random.nextInt(4000), CommonNames.auctionSearchActorName)))
      val buyer = testSystem.actorOf(Props(new Buyer(1.toString, keyWords.toVector, () => false, () => 1000 + Random.nextInt(2000), CommonNames.auctionSearchActorName)))
      val testActor = testSystem.actorOf(Props(new TestActor(seller, buyer)))
      testActor ! Start
      Await.result(testSystem.whenTerminated, Duration.Inf)
    }
  }


}


class TestActor(seller: ActorRef, buyer: ActorRef) extends Actor {

  var t1 = System.nanoTime()
  var t2 = System.nanoTime()
  var t3 = System.nanoTime()

  override def receive: Receive = {
    case Start =>
      t1 = System.currentTimeMillis()
      println("Start")
      seller ! Start
    case Done =>
      t2 = System.currentTimeMillis()
      println("Done")
      buyer ! Start
    case Stop =>
      t3 = System.currentTimeMillis()
      println("Stopped")
      println("Searching: " + (t3 - t2) + "[ms]")
      println("Whole: " + (t3 - t1) + "[ms]")
      context.system.terminate()
  }
}