package com.tuktuk

import java.util.UUID

import scala.concurrent.Future

import akka.actor.{ Props }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.directives.SecurityDirectives.AsyncAuthenticator
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ RawHeader, Accept, Location }
import akka.http.scaladsl.model.MediaTypes.`application/json`

import spray.json._

import org.scalatest.{ FunSuite, Matchers }
import com.tuktuk.server.WebServer

import org.mongodb.scala._
import org.mongodb.scala.bson.collection.immutable.Document
import com.mongodb.connection.ClusterSettings
import org.mongodb.scala.ServerAddress
import org.mongodb.scala.MongoClientSettings
import com.tuktuk.model.DriverLocation
import com.tuktuk.model.DriverJsonProtocol._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import com.tuktuk.model.TotalDrivers
import com.tuktuk.model.NearestDrivers
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import com.tuktuk.server.LogTracker


class ApiTests extends FunSuite
    with ScalatestRouteTest
    with Matchers with WebServer with LogTracker{

  implicit val actorRefFactory: ActorSystem = GlobalActorSystem.actorSystem
  override implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  var database: MongoDatabase = _
  implicit var driversCollection: MongoCollection[Document] = _

  override def beforeAll() {
    
    /**
     * The expectation is that Mongo server is already running on localhost @ 27017
     * with 'test' database created
     */
    val mongoClient: MongoClient = MongoClient()

    database = mongoClient.getDatabase("test");
    val collectionNames = database.listCollectionNames().toFuture()
    val names = Await.result(collectionNames, 10 seconds)
    val collectionExists =  names.forall { x => x=="testDrivers" }
    
    if(!collectionExists ) {
      val collectionCreation = database.createCollection("testDrivers").toFuture();
      Await.result(collectionCreation, 10 seconds)
    }
    driversCollection = database.getCollection("testDrivers")
    val indexFuture = driversCollection.createIndex(Document("location" -> "2dsphere")).toFuture()
    indexFuture.onSuccess {
      case _ => info("index created")
    }
    Await.result(indexFuture, 10 seconds)
  }

  override def afterAll() {
    val f = driversCollection.drop().toFuture()
    f.onSuccess {
      case _ => info("collection testDrivers dropped")
    }
    Await.result(f, 10 seconds)
  }

  def getDriverLocation(lat: Double, long: Double): DriverLocation = {
    DriverLocation(None, lat, long, 0.7)
  }

  /* Route test cases */
  
  test("Put location data of a driver 132 should be successful") {
    Put(s"/drivers/132/location", getDriverLocation(12.97161923, 108.59463452)) ~>
      Route.seal(compositeRoute) ~> check {
        status should be(StatusCodes.OK)
        val drivers = responseAs[String].parseJson.convertTo[TotalDrivers]
        assert(drivers.totalDrivers==1, s"Total drivers should be 1, instead they are ${drivers.totalDrivers} ")
      }
  }
  
    test("Put location data of a driver 123 should be successful") {
    Put(s"/drivers/123/location", getDriverLocation(12.97161924, 108.59463453)) ~>
      Route.seal(compositeRoute) ~> check {
        status should be(StatusCodes.OK)
        val drivers = responseAs[String].parseJson.convertTo[TotalDrivers]
        println("dd::" + drivers)
        assert(drivers.totalDrivers==2, s"Total drivers should be 1, instead they are ${drivers.totalDrivers} ")
      }
  }
  
  test("Get nearest drivers with-in 500 metres for a location should be successful") {
    driversCollection.createIndex(Document("location" -> "2dsphere"))
    Get(s"/drivers?latitude=12.97161923&longitude=108.59463452", "") ~>
      Route.seal(compositeRoute) ~> check {
        status should be(StatusCodes.OK)
        val neigbours = responseAs[String].parseJson.convertTo[NearestDrivers]
        assert(neigbours.drivers.map(x => x.size==2).getOrElse(false), "No drivers found")
      }
  }
  
  test("Get drivers with-in 100 metres for a location should be un-successful") {
    driversCollection.createIndex(Document("location" -> "2dsphere"))
    Get(s"/drivers?latitude=12.98&longitude=118.59463452&radius=100&limit=2", "") ~>
      Route.seal(compositeRoute) ~> check {
        status should be(StatusCodes.OK)
        val neigbours = responseAs[String].parseJson.convertTo[NearestDrivers]
        assert(neigbours.drivers.map(x => x.size==0).getOrElse(false), "Drivers found")
      }
  }

  /* Api request validation testcases */
  
  test("Lat/Long values cross boundaries for driver search - should be unsuccessful") {
    driversCollection.createIndex(Document("location" -> "2dsphere"))
    Get(s"/drivers?latitude=12.98&longitude=218.59463452&radius=100&limit=2", "") ~>
      Route.seal(compositeRoute) ~> check {
        status should be(StatusCodes.BadRequest)
      }
  }
  
   test("Put location data of a driver with id 5001 should be un-successful") {
    Put(s"/drivers/5001/location", getDriverLocation(12.97161923, 108.59463452)) ~>
      Route.seal(compositeRoute) ~> check {
        status should be(StatusCodes.BadRequest)
      }
  }
   
//  test("Put location data of a driver which crossed boundaries, should be un-successful") {
//    Put(s"/drivers/501/location", getDriverLocation(12.97161923, 208.59463452)) ~>
//      Route.seal(compositeRoute) ~> check {
//        status should be(StatusCodes.BadRequest)
//      }
//  } 

}