package actors.auctions

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, FSM}
import common.{AuctionSold, _}

import scala.concurrent.duration.FiniteDuration

/**
  * Created by wiktor on 27/10/16.
  */
class AuctionActorFSM(var startingPrice: BigDecimal, val seller: ActorRef, val auctionId: String)
  extends IAuction with FSM[AuctionState, DataState] {

  val startTime = FiniteDuration(3000, TimeUnit.MILLISECONDS)
  val keepAlive = FiniteDuration(1000, TimeUnit.MILLISECONDS)
  val deleteTime = FiniteDuration(3000, TimeUnit.MILLISECONDS)
  var currentLeader: ActorRef = null
  var notifyLeader: Boolean = false

  startWith(Created, Uninitialized)

  when(Created, stateTimeout = startTime) {
    case Event(Start, Uninitialized) =>
      stay using AuctionData(startingPrice)
    case Event(Stop | StateTimeout, _) =>
      goto(Ignored) using Uninitialized
    case Event(Bid(newBid, ifNotify), AuctionData(currentBid)) => newBid > currentBid match {
      case true =>
        this.notifyLeader = ifNotify
        this.currentLeader = sender
        goto(Activated) using AuctionData(newBid)
      case false =>
        sender ! BidFailed("To low bid, current bid: " + currentBid + ", your: " + newBid, currentBid)
        stay using AuctionData(currentBid)
    }
  }

  when(Ignored, stateTimeout = deleteTime) {
    case Event(Start, Uninitialized) =>
      goto(Created) using Uninitialized
    case Event(StateTimeout, _) =>
      seller ! AuctionNotSold(auctionId)
      context.stop(self)
      stay
  }

  when(Activated, stateTimeout = keepAlive) {
    case Event(Bid(newBid, ifNotify), AuctionData(currentBid)) => newBid > currentBid match {
      case true =>
        if (this.notifyLeader) {
          this.currentLeader ! Notify(newBid)
        }
        this.notifyLeader = ifNotify
        this.currentLeader = sender
        println("New auction leader with bid: " + newBid)
        stay using AuctionData(newBid)
      case false => sender ! BidFailed("To low bid, current bid: " + currentBid + ", your: " + newBid + " auction name: " + auctionId, currentBid)
        stay using AuctionData(currentBid)
    }
    case Event(StateTimeout, AuctionData(currentBid)) =>
      goto(Sold) using AuctionData(currentBid)
  }

  onTransition {
    case Activated -> Sold =>
      stateData match {
        case AuctionData(currentBid) =>
          this.currentLeader ! AuctionSold(sender, currentBid, auctionId)
          seller ! AuctionSold(sender, currentBid, auctionId)
        case _ =>
      }
  }

  when(Sold, stateTimeout = deleteTime) {
    case Event(StateTimeout, _) =>
      println("Auction " + auctionId + " has ended")
      context.stop(self)
      stay
  }

  whenUnhandled {
    case Event(e, s) => {
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
    }
  }

}

sealed trait AuctionState

case object Offline extends AuctionState

case object Created extends AuctionState

case object Activated extends AuctionState

case object Ignored extends AuctionState

case object Sold extends AuctionState


sealed trait DataState

case object Uninitialized extends DataState

case class AuctionData(currentBid: BigDecimal) extends DataState