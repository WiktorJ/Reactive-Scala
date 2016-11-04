package actors.auctions

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.LoggingReceive
import common._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

/**
  * Created by wiktor on 23/10/16.
  */
class AuctionActorBasic(var currentBid: BigDecimal, val seller: ActorRef, val auctionId: String) extends IAuction {

  val startTime = FiniteDuration(3000, TimeUnit.MILLISECONDS)
  val deleteTime = FiniteDuration(3000, TimeUnit.MILLISECONDS)

  var deleteTimer: Cancellable = null
  var startTimer: Cancellable = null
  var currentLeader: ActorRef = null

  override def receive: Receive = created

  def created: Receive = LoggingReceive {
    case Start => startTimer = context.system.scheduler.scheduleOnce(startTime) {
      self ! Stop
    }

    case Stop =>
      context become ignored
      deleteTimer = context.system.scheduler.scheduleOnce(deleteTime) {
        seller ! AuctionNotSold(auctionId)
        context.stop(self)
      }

    case Bid(amount) => if (amount > currentBid) {
      this.currentLeader = sender
      currentBid = amount
      context become activated
    } else {
      sender ! BidFailed("To low bid, current bid: " + currentBid + ", your: " + amount, currentBid)
    }

  }

  def ignored: Receive = LoggingReceive {
    case Start => deleteTimer.cancel()
      context become created
      self ! Start
  }

  def activated: Receive = LoggingReceive {
    case Bid(amount) => if (amount > currentBid) {
      currentLeader = sender
      currentBid = amount
      context become activated
    } else {
      sender ! BidFailed("To low bid, current bid: " + currentBid + ", your: " + amount, currentBid)
    }
    case Stop =>
      this.currentLeader ! AuctionSold(sender, currentBid, auctionId)
      seller ! AuctionSold(sender, currentBid, auctionId)
      context become sold
      deleteTimer = context.system.scheduler.scheduleOnce(deleteTime) {
        self ! Stop
      }
  }

  def sold: Receive = LoggingReceive {
    case Stop => println("Auction " + auctionId + " has ended")
  }
}

