package common

import actors.auctions.{AuctionActorPersistFSM, AuctionActorPresistFSMPublish}
import akka.actor.{ActorRef, ActorRefFactory, Props}

/**
  * Created by wiktor on 09/11/16.
  */
class PublishFSMPersistAuctionFactory(val notifier: ActorRef) extends IAuctionFactory {
  override def produce(actorRefFactory: ActorRefFactory, currentBid: BigDecimal, seller: ActorRef, auctionName:String, auctionId: String, auctionSearchName: String): ActorRef =
    actorRefFactory.actorOf(Props(new AuctionActorPresistFSMPublish(currentBid, seller, auctionName, auctionId, auctionSearchName, notifier)))
}
