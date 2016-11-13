package common
import actors.auctions.{AuctionActorFSM, AuctionActorPersistFSM}
import akka.actor.{ActorRef, ActorRefFactory, Props}

/**
  * Created by wiktor on 09/11/16.
  */
class FSMPersistAuctionFactory extends IAuctionFactory {
  override def produce(actorRefFactory: ActorRefFactory, currentBid: BigDecimal, seller: ActorRef, auctionName:String, auctionId: String, auctionSearchName: String): ActorRef =
    actorRefFactory.actorOf(Props(new AuctionActorPersistFSM(currentBid, seller, auctionName, auctionId, auctionSearchName)))
}
