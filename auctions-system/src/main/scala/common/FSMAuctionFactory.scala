package common
import actors.auctions.{AuctionActorFSM, IAuction}
import akka.actor.ActorRef

/**
  * Created by wiktor on 28/10/16.
  */
class FSMAuctionFactory extends IAuctionFactory{
  override def produce(currentBid: BigDecimal, seller: ActorRef, auctionId: String): IAuction =  new AuctionActorFSM(currentBid, seller, auctionId)
}
