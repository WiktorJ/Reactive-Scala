package actors

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import common.{AddAuction, GetAuctions, RemoveAuction, ResponseWithAuctions}

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

/**
  * Created by wiktor on 03/11/16.
  */
class AuctionSearch extends Actor {
  var auctions: ArrayBuffer[AuctionActorWrapper] = ArrayBuffer()

  override def receive: Receive = {
    case AddAuction(name, auction) =>
      auctions.synchronized {
          auctions += new AuctionActorWrapper(name, auction)
      }
    case GetAuctions(key) =>
      sender ! ResponseWithAuctions(auctions.filter(a => a.name.contains(key)).toList)
    case RemoveAuction(name) =>
      auctions.synchronized {
        auctions = auctions.filter(a => !a.name.eq(name))
      }
  }


}

class AuctionActorWrapper(val name: String, val actor: ActorRef) {}

