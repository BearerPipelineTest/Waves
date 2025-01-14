package com.wavesplatform.api.http.alias

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.Route
import cats.syntax.either.*
import com.wavesplatform.account.Alias
import com.wavesplatform.api.common.CommonTransactionsApi
import com.wavesplatform.api.http.requests.CreateAliasRequest
import com.wavesplatform.api.http.{BroadcastRoute, *}
import com.wavesplatform.network.TransactionPublisher
import com.wavesplatform.settings.RestAPISettings
import com.wavesplatform.state.Blockchain
import com.wavesplatform.transaction.*
import com.wavesplatform.utils.Time
import com.wavesplatform.wallet.Wallet
import play.api.libs.json.{JsString, Json}

case class AliasApiRoute(
    settings: RestAPISettings,
    commonApi: CommonTransactionsApi,
    wallet: Wallet,
    transactionPublisher: TransactionPublisher,
    time: Time,
    blockchain: Blockchain,
    routeTimeout: RouteTimeout
) extends ApiRoute
    with BroadcastRoute
    with AuthRoute {

  override val route: Route = pathPrefix("alias") {
    addressOfAlias ~ aliasOfAddress ~ deprecatedRoute
  }

  private def deprecatedRoute: Route =
    path("broadcast" / "create") {
      broadcast[CreateAliasRequest](_.toTx)
    } ~ (path("create") & withAuth) {
      broadcast[CreateAliasRequest](TransactionFactory.createAlias(_, wallet, time))
    }

  def addressOfAlias: Route = (get & path("by-alias" / Segment)) { aliasName =>
    complete {
      Alias
        .create(aliasName)
        .flatMap { a =>
          blockchain.resolveAlias(a).bimap(_ => TxValidationError.AliasDoesNotExist(a), addr => Json.obj("address" -> addr.toString))
        }
    }
  }

  private implicit val ess: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  def aliasOfAddress: Route = (get & path("by-address" / AddrSegment)) { address =>
    routeTimeout.executeStreamed {
      commonApi
        .aliasesOfAddress(address)
        .map { case (_, tx) => JsString(tx.alias.toString) }
        .toListL
    }(identity)
  }
}
