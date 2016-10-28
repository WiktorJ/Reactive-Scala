package common

import akka.actor.ActorRef

import scala.collection.mutable.ArrayBuffer

/**
  * Created by wiktor on 28/10/16.
  */
//TODO: I cry when I see this,
object AuctionsHolder {
  var auctions: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]()

  def add(actorRef: ActorRef) {
    auctions += actorRef
  }

  def get(index: Int): ActorRef = {
    auctions(index)
  }

  def remove(auction: ActorRef) = {
    auctions = auctions - auction
  }

  def size(): Int = {
    auctions.size
  }
}
