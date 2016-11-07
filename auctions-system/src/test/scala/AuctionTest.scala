import actors.Seller
import actors.auctions.{AuctionActorFSM, Created, Uninitialized}
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestFSMRef, TestKit}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

/**
  * Created by wiktor on 04/11/16.
  */
class AuctionTest extends TestKit(ActorSystem("Auction"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender
  with MockFactory {

  override def afterAll(): Unit = {
    system.terminate
  }

  val price: BigDecimal = 42
  val id: String = "some_id"
  val actor = mock[ActorRef]

  "An Auction" must {
    "start in Created state with Uninitialized data" in {
      val fsm = TestFSMRef(new AuctionActorFSM(price, actor, id))
      fsm.stateName == Created
      fsm.stateData == Uninitialized
    }
  }

}
