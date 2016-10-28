import akka.actor.{ActorRef, ActorSystem, Props}
import actors.{Buyer, Seller}
import common.{BasicAuctionFactory, FSMAuctionFactory, Start}

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
  val system = ActorSystem("Auctions")
  var auctions: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]()

//  val auctionFactory = new BasicAuctionFactory()
  val auctionFactory = new FSMAuctionFactory()

  for (i <- 0 to sellerAmount) {
    val seller: ActorRef = system.actorOf(Props(new Seller(auctions, i.toString, auctionFactory)))
    seller ! Start
  }

  for (i <- 0 to buyerAmount) {
    val buyer = system.actorOf(Props(new Buyer(auctions, i.toString, 10 + Random.nextInt(10))))
    buyer ! Start
  }

  Await.result(system.whenTerminated, Duration.Inf)
}