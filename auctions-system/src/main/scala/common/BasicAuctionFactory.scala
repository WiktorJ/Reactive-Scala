package common

import actors.auctions.{AuctionActorBasic, AuctionActorFSM, IAuction}
import akka.actor.ActorRef

/**
  * Created by wiktor on 28/10/16.
  */
class BasicAuctionFactory extends IAuctionFactory {
  override def produce(currentBid: BigDecimal, seller: ActorRef, auctionId: String): IAuction = new AuctionActorBasic(currentBid, seller, auctionId)
}
