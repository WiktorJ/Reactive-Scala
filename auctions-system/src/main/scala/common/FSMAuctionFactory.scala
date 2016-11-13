package common

import actors.auctions.{AuctionActorFSM, IAuction}
import akka.actor.{ActorRef, ActorRefFactory, Props}

/**
  * Created by wiktor on 28/10/16.
  */
class FSMAuctionFactory extends IAuctionFactory {
  override def produce(actorRefFactory: ActorRefFactory, currentBid: BigDecimal, seller: ActorRef, auctionName:String, auctionId: String, auctionSearchName: String): ActorRef =
    actorRefFactory.actorOf(Props(new AuctionActorFSM(currentBid, seller, auctionId)))
}
