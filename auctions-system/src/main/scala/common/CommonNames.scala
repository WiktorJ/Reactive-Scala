package common

/**
  * Created by wiktor on 03/11/16.
  */
object CommonNames {
  val systemName: String = "Auctions"
  val auctionSearchActorName: String = "auctionSearch"

  val publishServiceConfig = "publishService"
  val localServiceConfig = "localService"
  val publishServiceName = "publisher"

  val publishServiceAddress = "akka.tcp://Auctions@127.0.0.1:2552/user/" + publishServiceName
}