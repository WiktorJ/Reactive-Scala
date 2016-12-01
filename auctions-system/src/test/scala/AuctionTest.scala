
import actors.{AuctionActorWrapper, AuctionSearch, Buyer, Seller}
import actors.auctions._
import akka.actor.FSM.StateTimeout
import akka.actor.{Actor, ActorRef, ActorRefFactory, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestFSMRef, TestKit, TestProbe}
import common._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}

import scala.concurrent.duration.DurationInt

/**
  * Created by wiktor on 04/11/16.
  */
class AuctionTest extends TestKit(ActorSystem(CommonNames.systemName))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  override def afterAll(): Unit = {
    system.terminate
  }

  val price: BigDecimal = 42
  val id: String = "some_id"

  "An Auction" must {
    "start in Created state with Uninitialized data" in {
      val sellerProbe = TestProbe()
      val fsm = TestFSMRef(new AuctionActorFSM(price, sellerProbe.ref, id))
      fsm.stateName == Created
      fsm.stateData == Uninitialized
    }

    "change state to Ignored when received Stop" in {
      val sellerProbe = TestProbe()
      val fsm = TestFSMRef(new AuctionActorFSM(price, sellerProbe.ref, id))
      fsm ! Stop
      fsm.stateName == Ignored
      fsm.stateData == Uninitialized
    }

    "send AuctionNotSold message to its parent" in {
      val sellerProbe = TestProbe()
      val fsm = TestFSMRef(new AuctionActorFSM(price, sellerProbe.ref, id))
      fsm ! Stop
      fsm ! StateTimeout
      sellerProbe.expectMsg(500 millis, AuctionNotSold(id))
    }

    "send AuctionSold message to its parent and goto Sold state" in {
      val sellerProbe = TestProbe()
      val fsm = TestFSMRef(new AuctionActorFSM(price, sellerProbe.ref, id))
      fsm ! Start
      fsm ! Bid(price + 10, notifyBuyer = false)
      fsm ! StateTimeout
      sellerProbe.expectMsgPF(500 millis) {
        case AuctionSold(_, newPrice, _) if newPrice == price + 10 => ()
      }
      fsm.stateName == Sold
    }

    "send Notify to current leader" in {
      val sellerProbe = TestProbe()
      val probeBuyer = TestProbe()
      val probeBuyer2 = TestProbe()
      val buyer = TestProbe()
      val fsm = TestFSMRef(new AuctionActorFSM(price, sellerProbe.ref, id))
      fsm.tell(Start, buyer.ref)
      fsm.tell(Bid(price + 10, notifyBuyer = true), buyer.ref)
      buyer.expectNoMsg(500 millis)
      fsm.tell(Bid(price + 20, notifyBuyer = true), probeBuyer.ref)
      buyer.expectMsgPF(200 millis) {
        case Notify(newPrice) if newPrice == price + 20 => ()
      }
      fsm.tell(Bid(price + 20, notifyBuyer = false), probeBuyer2.ref)
      probeBuyer2.expectMsgPF(200 millis) {
        case BidFailed(_, currentPrice) if currentPrice == price + 20 => ()
      }
      fsm.tell(Bid(price + 30, notifyBuyer = false), probeBuyer2.ref)
      probeBuyer2.expectNoMsg(200 millis)
      probeBuyer.expectMsgPF(500 millis) {
        case Notify(newPrice) if newPrice == price + 30 => ()
      }
    }
  }

  "A Seller " must {

    "send Start message to Auction" in {
      val auctionProbe = TestProbe()
      val seller = TestActorRef(Props(new Seller(id, new IAuctionFactory {
        override def produce(actorRefFactory: ActorRefFactory, currentBid: BigDecimal, seller: ActorRef, auctionName: String, auctionId: String, auctionSearchName: String): ActorRef = auctionProbe.ref
      }, Array("random_name"), () => 10, "0")))
      seller.underlyingActor
      seller ! Start
      auctionProbe.expectMsg(500 millis, Start)
    }

    "send AddAuction to AuctionSearch" in {
      val auctionSearch = TestProbe()
      TestActorRef(ForwardActor.props(auctionSearch.ref), "1")
      val seller = TestActorRef(Props(new Seller(id, new IAuctionFactory {
        override def produce(actorRefFactory: ActorRefFactory, currentBid: BigDecimal, seller: ActorRef, auctionName: String, auctionId: String, auctionSearchName: String): ActorRef = TestProbe().ref
      }, Array("random_name"), () => 10, "1")))
      seller ! Start
      auctionSearch.expectMsgPF(500 millis) {
        case AddAuction(_, _) => ()
      }
    }

    "send RemoveAuction to AuctionSearch" in {
      val auctionSearch = TestProbe()
      TestActorRef(ForwardActor.props(auctionSearch.ref), "2")
      val seller = TestActorRef(Props(new Seller(id, new IAuctionFactory {
        override def produce(actorRefFactory: ActorRefFactory, currentBid: BigDecimal, seller: ActorRef, auctionName: String, auctionId: String, auctionSearchName: String): ActorRef = TestProbe().ref
      }, Array("random_name"), () => 10, "2")))
      seller ! AuctionNotSold(id)
      auctionSearch.expectMsgPF(500 millis) {
        case RemoveAuction(nid) if id == nid => ()
      }
    }
  }

  "An AuctionSearch" must {
    "send ResponseWithAuctions when receive GetAuctions" in {
      val probeBuyer = TestProbe()
      val auctionSearch = TestActorRef(Props(new AuctionSearch()))
      auctionSearch.tell(GetAuctions(""), probeBuyer.ref)
      probeBuyer.expectMsgPF(500 millis) {
        case ResponseWithAuctions(_) => ()
      }
    }
  }

  "A Buyer" must {
    "top bid if notified" in {
      val auctionProbe = TestProbe()
      val auctionSearch = TestProbe()
      system.actorOf(ForwardActor.props(auctionSearch.ref, auctionProbe.ref), "3")
      val buyer = system.actorOf(Props(new Buyer(id, Vector("some", "words"), () => true, () => 300, "3")))
      var currentBid: BigDecimal = 0
      buyer ! Start
      auctionSearch.expectMsgPF(500 millis) {
        case GetAuctions(_) => ()
      }

      auctionProbe.expectMsgPF(500 millis) {
        case Bid(newBid, true) =>
          currentBid = newBid
      }
      buyer.tell(Notify(currentBid + 10), auctionProbe.ref)
      auctionProbe.expectMsgPF(500 millis) {
        case Bid(newBid, true) if newBid > currentBid + 10 => ()
      }
    }
    "top bid if bid failed" in {
      val auctionProbe = TestProbe()
      val auctionSearch = TestProbe()
      system.actorOf(ForwardActor.props(auctionSearch.ref, auctionProbe.ref), "4")
      val buyer = system.actorOf(Props(new Buyer(id, Vector("some", "words"), () => true, () => 300, "4")))
      var currentBid: BigDecimal = 0
      buyer ! Start
      auctionSearch.expectMsgPF(500 millis) {
        case GetAuctions(_) => ()
      }

      auctionProbe.expectMsgPF(500 millis) {
        case Bid(newBid, true) =>
          currentBid = newBid
      }
      buyer.tell(BidFailed("some reason", currentBid + 10), auctionProbe.ref)
      auctionProbe.expectMsgPF(500 millis) {
        case Bid(newBid, true) if newBid > currentBid + 10 => ()
      }
    }
  }
}

object ForwardActor {
  def props(to: ActorRef) = Props(new ForwardActor(to))

  def props(to: ActorRef, probe: ActorRef) = Props(new ForwardActor(to, probe))
}

class ForwardActor(to: ActorRef, probe: ActorRef) extends Actor {
  def this(to: ActorRef) = {
    this(to, null)
  }

  override def receive = {
    case GetAuctions(x) =>
      sender ! ResponseWithAuctions(List(new AuctionActorWrapper("name", probe)))
      to forward GetAuctions(x)
    case x => to forward x
  }
}
