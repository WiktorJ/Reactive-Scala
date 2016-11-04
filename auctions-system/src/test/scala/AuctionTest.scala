import actors.auctions.AuctionActorFSM
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestFSMRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

/**
  * Created by wiktor on 04/11/16.
  */
class AuctionTest extends TestKit(ActorSystem("Auction"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  override def afterAll(): Unit = {
    system.terminate
  }

  "An Auction" must {
    "start in Created state with Uninitialized data" in {
//      val fsm = TestFSMRef(new AuctionActorFSM(_, _, _))
//      val mustBeTypedProperly: TestActorRef[TestFsmActor] = fsm
    }
  }

}
