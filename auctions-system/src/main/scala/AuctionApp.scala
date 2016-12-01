import akka.actor.{ActorRef, ActorSystem, Props}
import actors._
import com.typesafe.config.ConfigFactory
import common._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random



/**
  * Created by wiktor on 28/10/16.
  */

object AuctionApp extends App {

  def onlyOneAuction(): Long = {
    var toReturn: Long = 100
    if (!createAuction) {
      toReturn = 10000000000l
    }
    createAuction = false
    toReturn
  }


  val sellerAmount = 3
  val buyerAmount = 15
  val config = ConfigFactory.load()
  var system = ActorSystem(CommonNames.systemName, config.getConfig(CommonNames.localServiceConfig).withFallback(config))
  var publishSystem = ActorSystem(CommonNames.systemName, config.getConfig(CommonNames.publishServiceConfig).withFallback(config))
  var auctions: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]()
  var createAuction = true

  val notifier = system.actorOf(Props[Notifier])

  val auctionFactory = new PublishFSMPersistAuctionFactory(notifier)
//  val auctionFactory = new FSMPersistAuctionFactory()
//    val auctionFactory = new FSMAuctionFactory()
  //    val auctionFactory = new BasicA=>uctionFactory()

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

  publishSystem.actorOf(Props[AuctionPublisher], CommonNames.publishServiceName)

//  var searchActor = system.actorOf(Props(new MasterSearch(5)), CommonNames.auctionSearchActorName)
  var searchActor = system.actorOf(Props[AuctionSearch], CommonNames.auctionSearchActorName)
  //  val searchActor = system.actorOf(Props[PersistAuctionSearch], CommonNames.auctionSearchActorName)

  for (i <- 0 until sellerAmount) {
    val seller: ActorRef = system.actorOf(Props(new Seller(i.toString, auctionFactory, auctionNames, () => 2000 + Random.nextInt(4000), CommonNames.auctionSearchActorName)))
    seller ! Start
  }

  for (i <- 0 until buyerAmount) {
    val buyer = system.actorOf(Props(new Buyer(i.toString, keyWords.toVector, () => Random.nextInt(4) == 0, () => 1000 + Random.nextInt(2000), CommonNames.auctionSearchActorName)))
    buyer ! Start
  }

  Await.result(system.whenTerminated, Duration.Inf)

}