package com.tuktuk.server

import akka.http.scaladsl.model.ws.{ Message, TextMessage, BinaryMessage }
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.{ Flow, Source }
import akka.stream.scaladsl.Sink
import akka.stream.ActorMaterializer
import com.tuktuk.model.DriverJsonProtocol._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.actor.ActorSystem
import com.tuktuk.GlobalActorSystem

import spray.json.DefaultJsonProtocol
import com.tuktuk.model.DriverMongoProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.tuktuk.model._
import scala.concurrent.Future
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.Observable
import com.mongodb.async.client.Observable
import scala.concurrent.ExecutionContext
import org.mongodb.scala.Completed
import org.mongodb.scala.Observer
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.UpdateOptions

trait WebServer extends Directives {
  implicit val materializer: ActorMaterializer
  implicit val actorSystem = GlobalActorSystem.actorSystem
  implicit val ec: ExecutionContext = actorSystem.dispatcher

  private lazy val updateDriverLocationPath = path( IntNumber / "location") & put & pathEndOrSingleSlash &
    entity(as[DriverLocation])
  private lazy val getDriversPath = get & pathEndOrSingleSlash &
    parameters('latitude.as[Double], 'longitude.as[Double], 'radius.as[Int] ? 500, 'limit.as[Int] ? 10)

  def route(implicit collection: MongoCollection[Document]) = pathPrefix("drivers") {
    updateDriverLocationPath {
    (driverId, driverLocation) => {
      println(s"Inside driver location path---${driverLocation}")
      val driverLoc = driverLocation.copy(driverId=Some(driverId))
      complete(updateLocation(driverLoc))
    }
      
    } ~ getDriversPath {
      (lat, long, radius, limit) => {
        println("Inside get driver path---")
        val searchC = SearchCriteria(lat, long, radius, limit)
        complete(getNearestDrivers(searchC))
      }  
    }
  }

  private def updateLocation(location: DriverLocation)(implicit collection: MongoCollection[Document]): Future[TotalDrivers] =  {
    println("Before insertion into mongo")
    val observable = collection.replaceOne(equal("driverId", location.driverId.get),
        driverLocationToDocument(location),UpdateOptions().upsert(true) )
    val insertAndCount = for {
      insertResult <- observable
      countResult <- collection.count()
    } yield countResult
    insertAndCount.toFuture().map(x =>TotalDrivers(x.seq.head))
  }
  
  private def getNearestDrivers(criteria: SearchCriteria)(implicit collection: MongoCollection[Document]): Future[Seq[Driver]] = {
    val observable = collection.aggregate(List(searchCriteriaToDocument(criteria))).toFuture()
    val drivers = observable.map(x => x.map(doc => documentToDriver(doc)))
    drivers
  }
}
