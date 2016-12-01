package actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive
import akka.routing._
import common.{AddAuction, GetAuctions, RemoveAuction, ResponseWithAuctions}

import scala.concurrent.duration.FiniteDuration

/**
  * Created by wiktor on 30/11/16.
  */
class MasterSearch(val numberOfWorkers: Int) extends Actor {


  val workers = Vector.fill(numberOfWorkers) {
    val r = context.actorOf(Props[AuctionSearch])
    context watch r
    ActorRefRoutee(r)
  }

  val sellersRouter: Router = {
    Router(BroadcastRoutingLogic(), workers)
  }

  val buyersRouter: Router = {
    Router(ScatterGatherFirstCompletedRoutingLogic(within = FiniteDuration(1, TimeUnit.SECONDS)), workers)
  }

  override def receive: Receive = {
    case AddAuction(name, auction) =>
      sellersRouter.route(AddAuction(name, auction), sender)
    case GetAuctions(key) =>
      buyersRouter.route(GetAuctions(key), sender)
    case RemoveAuction(name) =>
      sellersRouter.route(RemoveAuction(name), sender)
  }
}
