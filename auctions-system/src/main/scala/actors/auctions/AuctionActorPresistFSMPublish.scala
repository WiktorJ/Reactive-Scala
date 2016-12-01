package actors.auctions

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import common.{AuctionSold, _}

import scala.concurrent.duration.FiniteDuration
import scala.reflect._
import scala.reflect.ClassTag


/**
  * Created by wiktor on 27/10/16.
  */
class AuctionActorPresistFSMPublish(var startingPrice: BigDecimal, val seller: ActorRef, val auctionName: String, val auctionId: String, val auctionSearchName: String, val notifier: ActorRef)
  extends IAuction with PersistentFSM[PersistAuctionState, DataState, DomainEvent] {

  val auctionTime = FiniteDuration(8, TimeUnit.SECONDS)
  val ignoreTime = FiniteDuration(3, TimeUnit.SECONDS)
  val deleteTime = FiniteDuration(3, TimeUnit.SECONDS)

  val timerName = "timer" + auctionId

  var currentLeader: ActorRef = null
  var notifyLeader: Boolean = false

  val searchActor = context.actorSelection("../" + auctionSearchName)

  var currentFinishAuctionTime: Long = 0
  var currentFinishIgnoreTime: Long = 0
  var currentFinishSoldTime: Long = 0

  startWith(PersistCreated, Uninitialized)

  when(PersistCreated) {
    case Event(Start, _) =>
      setTimer(timerName, TimeOut, auctionTime, repeat = false)
      currentFinishAuctionTime = System.currentTimeMillis / 1000 + auctionTime.length
      stay applying SetNewBid(startingPrice, currentFinishAuctionTime)
    case Event(Stop | TimeOut, _) =>
      setTimer(timerName, TimeOut, ignoreTime, repeat = false)
      currentFinishIgnoreTime = System.currentTimeMillis / 1000 + ignoreTime.length
      goto(PersistIgnored) applying StayInCurrentState(currentFinishIgnoreTime)
    case Event(Bid(newBid, ifNotify), AuctionData(currentBid)) => newBid > currentBid match {
      case true =>
        this.notifyLeader = ifNotify
        this.currentLeader = sender
        goto(PersistActivated) applying SetNewBid(newBid, currentFinishAuctionTime)
      case false =>
        sender ! BidFailed("To low bid, current bid: " + currentBid + ", your: " + newBid, currentBid)
        stay applying StayInCurrentState(currentFinishAuctionTime)
    }
  }

  when(PersistIgnored) {
    case Event(Start, _) =>
      setTimer(timerName, TimeOut, auctionTime, repeat = false)
      currentFinishAuctionTime = System.currentTimeMillis / 1000 + auctionTime.length
      goto(PersistCreated) applying StayInCurrentState(currentFinishAuctionTime)
    case Event(TimeOut, _) =>
      seller ! AuctionNotSold(auctionName)
      setTimer(timerName, TimeOut, deleteTime, repeat = false)
      currentFinishSoldTime = System.currentTimeMillis / 1000 + deleteTime.length
      goto(PersistSold) applying Finish(currentFinishSoldTime)
    case Event(Bid(_, _), _) =>
      sender ! AuctionNotAvailableForBidding
      stay applying StayInCurrentState(currentFinishIgnoreTime)
  }

  when(PersistActivated) {
    case Event(Bid(newBid, ifNotify), AuctionData(currentBid)) => newBid > currentBid match {
      case true =>
        if (this.notifyLeader) {
          if (this.currentLeader != null) {
            this.currentLeader ! Notify(newBid)
          } else {
            println("No leader to send, this is probably due to lack of persistence of buyer actor")
          }
        }
        this.notifyLeader = ifNotify
        this.currentLeader = sender
        //        println("New auction leader with bid: " + newBid)
        goto(PersistActivated) applying SetNewBid(newBid, currentFinishAuctionTime)
      case false => sender ! BidFailed("To low bid, current bid: " + currentBid + ", your: " + newBid + " auction name: " + auctionId, currentBid)
        stay applying StayInCurrentState(currentFinishAuctionTime)
    }
    case Event(TimeOut, AuctionData(currentBid)) =>
      if (this.currentLeader != null) {
        this.currentLeader ! AuctionSold(sender, currentBid, auctionName)
      } else {
        println("No leader to send, this is probably due to lack of persistence of buyer actor")
      }
      seller ! AuctionSold(sender, currentBid, auctionName)
      setTimer(timerName, TimeOut, deleteTime, repeat = false)
      currentFinishSoldTime = System.currentTimeMillis / 1000 + deleteTime.length
      goto(PersistSold) applying Finish(currentFinishSoldTime)
  }


  when(PersistSold) {
    case Event(Bid(_, _), _) =>
      sender ! AuctionNotAvailableForBidding
      stay applying StayInCurrentState(currentFinishSoldTime)
    case Event(TimeOut, _) =>
      println("Auction " + auctionId + " has ended")
      context.stop(self)
      stay
  }


  override type Timeout = Option[FiniteDuration]

  override def applyEvent(event: DomainEvent, dataBeforeEvent: DataState): DataState = {
    event match {
      case SetNewBid(newBid, time) =>
        publishState(newBid)
        updateCurrentTimes(time)
        AuctionData(newBid)
      case StayInCurrentState(time) =>
        updateCurrentTimes(time)
        dataBeforeEvent
      case Finish(time) =>
        searchActor ! RemoveAuction(auctionName)
        updateCurrentTimes(time)
        dataBeforeEvent
    }
  }


  def updateCurrentTimes(time: Long): Unit = {
    val timeLeft = FiniteDuration(time - (System.currentTimeMillis / 1000), TimeUnit.SECONDS);
    //    println(time - (System.currentTimeMillis / 1000))
    if (timeLeft.length > 0) {
      setTimer(timerName, TimeOut, timeLeft, repeat = false)
      currentFinishAuctionTime = time
      currentFinishIgnoreTime = time
      currentFinishSoldTime = time
    } else {
      setTimer(timerName, TimeOut, FiniteDuration(0, TimeUnit.SECONDS), repeat = false)
    }
  }


  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  def publishState(currentBid: BigDecimal): Unit = {
    if (this.currentLeader != null) {
      this.notifier ! PublisherNotify(this.auctionName, this.currentLeader.toString(), currentBid)
    }
  }

  override def domainEventClassTag: ClassTag[DomainEvent] = classTag[DomainEvent]

  override def persistenceId: String = "auction-actor-persist-fsm-" + auctionId

}
