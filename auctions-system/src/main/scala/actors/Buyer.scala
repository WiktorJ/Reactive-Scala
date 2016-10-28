package actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem}
import common._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by wiktor on 28/10/16.
  */


class Buyer(val auctions: ArrayBuffer[ActorRef], val buyerId: String, var bidsAmount: Int) extends Actor {


  override def receive: Receive = {
    case Start => {
      context.system.scheduler.scheduleOnce(FiniteDuration(500 + Random.nextInt(5000), TimeUnit.MILLISECONDS)) {
        try {
//          auctions(Random.nextInt(auctions.size)) ! Bid(BigDecimal(50 + Random.nextInt(200)))
          AuctionsHolder.get(Random.nextInt(AuctionsHolder.size())) ! Bid(BigDecimal(50 + Random.nextInt(200)))
        } catch {
          case _: IllegalArgumentException => println("No auctions to bid")
        }
        self ! Start
      }
    }
    case Stop =>
      println("Buyer " + buyerId + " Finished buying")
    case BidFailed(reason, currentBit) =>
      println("Bid failed for buyer " + buyerId + " reson: " + reason)
      sender ! Bid(currentBit + 10)
    case AuctionSold(_, price, auctionId) => println("Buyer " + buyerId + " bought item" + auctionId + " for " + price)

  }
}

