package common

import actors.auctions.IAuction
import akka.actor.ActorRef

/**
  * Created by wiktor on 28/10/16.
  */
trait IAuctionFactory {
  def produce(currentBid: BigDecimal, seller: ActorRef, auctionId: String): IAuction
}
