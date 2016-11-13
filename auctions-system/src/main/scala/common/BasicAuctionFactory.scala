package common

import actors.auctions.{AuctionActorBasic, AuctionActorFSM, IAuction}
import akka.actor.{ActorRef, ActorRefFactory, Props}

/**
  * Created by wiktor on 28/10/16.
  */
class BasicAuctionFactory extends IAuctionFactory {
  override def produce(actorRefFactory: ActorRefFactory, currentBid: BigDecimal, seller: ActorRef, auctionName:String, auctionId: String, auctionSearchName: String): ActorRef =
    actorRefFactory.actorOf(Props(new AuctionActorBasic(currentBid, seller, auctionId)))
}
