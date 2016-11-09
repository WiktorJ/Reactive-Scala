import akka.actor.{ActorRef, ActorSystem, Props}
import actors.{AuctionSearch, Buyer, Seller}
import common._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

/**
  * Created by wiktor on 28/10/16.
  */

object AuctionApp extends App {
  val sellerAmount = 3
  val buyerAmount = 15
  val system = ActorSystem(CommonNames.systemName)
  var auctions: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]()

  val auctionFactory = new FSMAuctionFactory()
  //  val auctionFactory = new BasicAuctionFactory()

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

  val searchActor = system.actorOf(Props[AuctionSearch], CommonNames.auctionSearchActorName)

  for (i <- 0 to sellerAmount) {
    val seller: ActorRef = system.actorOf(Props(new Seller(i.toString, auctionFactory, auctionNames, () => 1000 + Random.nextInt(2000))))
    seller ! Start
  }

  for (i <- 0 to buyerAmount) {
    val buyer = system.actorOf(Props(new Buyer(i.toString, keyWords.toVector, () => Random.nextInt(4) == 0, () => 500 + Random.nextInt(5000))))
    buyer ! Start
  }

  Await.result(system.whenTerminated, Duration.Inf)
}