package common

import akka.actor.{Actor, ActorRef}

/**
  * Created by wiktor on 23/10/16.
  */
sealed trait AuctionFlow

case class InitializeAuction(currentBid: BigDecimal,
                      seller: ActorRef,
                      auctionId: String) extends AuctionFlow

case class Bid(amount: BigDecimal) extends AuctionFlow {
  require(amount > 0)
}

case object Start extends AuctionFlow

case object Stop extends AuctionFlow

case object Notify extends AuctionFlow


sealed trait AuctionResponses

case class BidFailed(reason: String, currentBit: BigDecimal)

case class AuctionSold(winner: ActorRef, price: BigDecimal, auctionId: String)

case class AuctionNotSold(auctionId: String)