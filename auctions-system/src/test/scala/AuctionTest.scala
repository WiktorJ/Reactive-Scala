import java.util.concurrent.TimeUnit

import actors.Seller
import actors.auctions._
import akka.actor.FSM.StateTimeout
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestFSMRef, TestKit, TestProbe}
import common._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
  * Created by wiktor on 04/11/16.
  */
class AuctionTest extends TestKit(ActorSystem("Auction"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  override def afterAll(): Unit = {
    system.terminate
  }

  val price: BigDecimal = 42
  val id: String = "some_id"
  val sellerProbe = TestProbe()

  "An Auction" must {
    "start in Created state with Uninitialized data" in {
      val fsm = TestFSMRef(new AuctionActorFSM(price, sellerProbe.ref, id))
      fsm.stateName == Created
      fsm.stateData == Uninitialized
    }

    "change state to Ignored when received Stop" in {
      val fsm = TestFSMRef(new AuctionActorFSM(price, sellerProbe.ref, id))
      fsm ! Stop
      fsm.stateName == Ignored
      fsm.stateData == Uninitialized
    }

    "send AuctionNotSold message to its parent" in {
      val fsm = TestFSMRef(new AuctionActorFSM(price, sellerProbe.ref, id))
      fsm ! Stop
      fsm ! StateTimeout
      sellerProbe.expectMsg(500 millis, AuctionNotSold(id))
    }

    "send AuctionSold message to its parent and goto Sold state" in {
      val fsm = TestFSMRef(new AuctionActorFSM(price, sellerProbe.ref, id))
      fsm ! Start
      fsm ! Bid(price + 10)
      fsm ! StateTimeout
      sellerProbe.expectMsgPF(500 millis) {
        case AuctionSold(_, newPrice, _) if newPrice == price + 10 => ()
      }
      fsm.stateName == Sold
    }
  }

}
