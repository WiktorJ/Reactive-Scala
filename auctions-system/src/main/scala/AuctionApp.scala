import akka.actor.{ActorRef, ActorSystem, Props}
import actors.{AuctionSearch, AuctionsIdGenerator, Buyer, Seller}
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
  var system = ActorSystem(CommonNames.systemName)
  var auctions: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]()
  var createAuction = true

  val auctionFactory = new FSMPersistAuctionFactory()
  //  val auctionFactory = new FSMAuctionFactory()
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

  var searchActor = system.actorOf(Props[AuctionSearch], CommonNames.auctionSearchActorName)
  //  val searchActor = system.actorOf(Props[PersistAuctionSearch], CommonNames.auctionSearchActorName)

  import java.io._
  var pw = new BufferedWriter(new FileWriter("/home/wiktor/Studies/Scala/solutions-repository/auctions-system/src/main/resources/system_shutdown_time.txt" , true))
  pw.write("S: " + System.currentTimeMillis.toString)
  pw.write("\n")
  pw.close()

  for (i <- 0 until sellerAmount) {
    val seller: ActorRef = system.actorOf(Props(new Seller(i.toString, auctionFactory, auctionNames, () => 2000, CommonNames.auctionSearchActorName)))
    seller ! Start
  }

  for (i <- 0 until buyerAmount) {
    val buyer = system.actorOf(Props(new Buyer(i.toString, keyWords.toVector, () => Random.nextInt(4) == 0, () => 1000, CommonNames.auctionSearchActorName)))
    buyer ! Start
  }

  Await.result(system.whenTerminated, Duration.Inf)

}