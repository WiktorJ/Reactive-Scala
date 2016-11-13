package common

import actors.auctions.IAuction
import akka.actor.{ActorRef, ActorRefFactory}

/**
  * Created by wiktor on 28/10/16.
  */
trait IAuctionFactory {
  def produce(actorRefFactory: ActorRefFactory, currentBid: BigDecimal, seller: ActorRef, auctionName:String, auctionId: String, auctionSearchName: String): ActorRef
}
