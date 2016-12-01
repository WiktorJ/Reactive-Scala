package actors

import akka.actor.Actor
import common.PublisherNotify

import scala.util.Random

/**
  * Created by wiktor on 29/11/16.
  */
class AuctionPublisher extends Actor {

  override def receive: Receive = {
    case PublisherNotify(title, currentBuyer, currentBid) =>
      publish(title, currentBuyer, currentBid)
  }

  def publish(title: String, currentBuyer: String, currentBid: BigDecimal): Unit = {
    val r = Random.nextInt(50)
    if (r == 1 || r == 2) {
      throw new PublishingException()
    }
    if (r == 3) {
      throw new PublishingSystemShutdownException()
    }
    println("[PUBLISHER] Auction " + title + " has new top buyer " + currentBuyer + " with bid " + currentBid)
  }
}


class PublishingException extends Exception("Error while publishing")
class PublishingSystemShutdownException extends Exception("Publishing system is dead")