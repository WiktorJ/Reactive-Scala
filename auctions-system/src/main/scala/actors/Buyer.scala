package actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable}
import akka.event.LoggingReceive
import common._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Random, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future

/**
  * Created by wiktor on 28/10/16.
  */


class Buyer(val buyerId: String, var bidsAmount: Int, val keyWords: Vector[String]) extends Actor {
  val timeoutDuration = FiniteDuration(5, TimeUnit.SECONDS)
  implicit val timeout = Timeout(timeoutDuration)
  var maxBid: BigDecimal = 1000
  var restoreTimer: Cancellable = null


  override def receive: Receive = regular

  def regular: Receive = LoggingReceive {
    case Start =>
      if(restoreTimer != null) {
        restoreTimer.cancel()
      }
      context.system.scheduler.scheduleOnce(FiniteDuration(500 + Random.nextInt(5000), TimeUnit.MILLISECONDS)) {
        val searchResponse = context.actorSelection("../" + CommonNames.auctionSearchActorName) ?
          GetAuctions(keyWords(Random.nextInt(keyWords.size)))
        searchResponse.onComplete {
          case Success(ResponseWithAuctions(auctions)) =>
            try {
              val currentBid = BigDecimal(50 + Random.nextInt(200))
              auctions(Random.nextInt(auctions.size)) ! Bid(currentBid)
              if (Random.nextInt(4) == 0) {
                setRestoreTimeout()
                maxBid = currentBid + 300
                context become focus
              } else {
                self ! Start
              }
            } catch {
              case _: IllegalArgumentException => println("No auctions to bid")
                self ! Start
            }
          case _ => self ! Start
        }

      }
    case Stop =>
      println("Buyer " + buyerId + " Finished buying")
    case BidFailed(reason, currentBit) =>
      println("Bid failed for buyer " + buyerId + " reson: " + reason)
    //      sender ! Bid(currentBit + 10)
    case AuctionSold(_, price, auctionId) => println("Buyer " + buyerId + " bought item: " + auctionId + " for " + price)
  }

  def focus: Receive = LoggingReceive {
    case Notify(currentBid) =>
      topBid(currentBid)
    case BidFailed(reason, currentBid) =>
      println("Bid failed for buyer " + buyerId + " reson: " + reason)
      topBid(currentBid)
    case AuctionSold(_, price, auctionId) =>
      println("Buyer " + buyerId + " bought item: " + auctionId + " for " + price)
      context become regular
      self ! Start
  }

  def topBid(currentBid: BigDecimal): Unit = {
    setRestoreTimeout()
    if (currentBid < maxBid) {
      sender ! Bid(currentBid + 10)
    } else {
      context become regular
      self ! Start
    }
  }

  def setRestoreTimeout(): Unit = {
    if(restoreTimer != null) {
      restoreTimer.cancel()
    }
    restoreTimer = context.system.scheduler.scheduleOnce(timeoutDuration) {
      println("Buyer " + buyerId + " has stuck in focused. Restoring....")
      context become regular
      self ! Start
    }
  }

}

