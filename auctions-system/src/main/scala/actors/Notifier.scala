package actors

import akka.actor.{Actor, ActorInitializationException, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.actor.Actor.Receive
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.pattern.ask

import scala.concurrent.duration._
import akka.util.Timeout
import common.{CommonNames, Done, PublisherNotify}

import scala.concurrent.{Await, TimeoutException}

/**
  * Created by wiktor on 29/11/16.
  */
class Notifier extends Actor with ActorLogging {

  var pendingRequests: Map[ActorRef, ActorRef] = Map[ActorRef, ActorRef]()

  override def supervisorStrategy = OneForOneStrategy() {
    case _: PublishingException =>
      log.warning("Couldn't publish auction state. Retrying...")
      removeRequest(sender)
      Restart
    case _: ActorInitializationException => {
      log.warning("Timeout while publishing auction state. Retrying...")
      removeRequest(sender)
      Restart
    }
    case _: PublishingSystemShutdownException =>
      log.warning("Publishing system is currently not available. Message will not be published")
      removeRequest(sender)
      Stop
    case e =>
      log.warning("Unknown failure: " + e.getMessage)
      removeRequest(sender)
      Stop
  }

  override def receive: Receive = {
    case PublisherNotify(title, currentBuyer, currentBid) => {
      val request = context.actorOf(Props(new NotifierRequest(title, currentBuyer, currentBid)))
      pendingRequests += request -> sender
    }
    case Done => {
      println("[NOTIFIER] PUBLISHING ALL RIGHT")
      removeRequest(sender)
    }
  }

  def removeRequest(request: ActorRef): Unit = {
    pendingRequests -= request
  }
}

class NotifierRequest(title: String, currentBuyer: String, currentBid: BigDecimal) extends Actor {

  implicit val timeout = Timeout(5 seconds)

  override def receive: Receive = {
    case m => println("[NotifierRequest] received " + m)
  }

  override def preStart(): Unit = {
    val publisher = context.actorSelection(CommonNames.publishServiceAddress)
    val future = publisher ? PublisherNotify(title, currentBuyer, currentBid)
    Await.result(future, timeout.duration)
    context.parent ! Done
  }
}