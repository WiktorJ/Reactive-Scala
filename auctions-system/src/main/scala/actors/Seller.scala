package actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import auctions.AuctionActorBasic
import common._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by wiktor on 28/10/16.
  */
class Seller(val auctions: ArrayBuffer[ActorRef], val sellerId: String, val auctionFactory: IAuctionFactory) extends Actor {


  var amount: Int = 0

  override def receive: Receive = {
    case Start => context.system.scheduler.scheduleOnce(FiniteDuration(1000 + Random.nextInt(2000), TimeUnit.MILLISECONDS)) {
      val r = Random
      val auctionId = AuctionsIdGenerator.getNext()
      val actor = context.system.actorOf(Props(auctionFactory.produce(randBigDecimal(20, 100),
        self,
        sellerId + auctionId)), sellerId + auctionId)
      actor ! Start
      AuctionsHolder.add(actor)
//      auctions += actor
      self ! Start
    }
    case AuctionSold(_, price, auctionId) =>
      AuctionsHolder.remove(sender)
      println("Seller " + sellerId + " sold item" + auctionId + " for " + price)
    case AuctionNotSold(auctionId) =>
      AuctionsHolder.remove(sender)
      println("Seller " + sellerId + " Did not sell " + auctionId)
  }

  private def randBigDecimal(from: Int, to: Int): BigDecimal = {
    (Math.random() * (to - from)) + from
  }
}


object AuctionsIdGenerator {
  var seq = 0

  def getNext(): Int = {
    seq += 1
    seq
  }
}
