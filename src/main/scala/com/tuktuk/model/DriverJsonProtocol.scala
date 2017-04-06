package com.tuktuk.model

import spray.json._
import java.util.UUID
import scala.util.{ Try, Success, Failure }
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson._

/**
 * "latitude": 12.97161923, "longitude": 77.59463452, "accuracy": 0.7
 */
case class DriverLocation(driverId: Option[Int], latitude: Double, longitude: Double, accuracy: Double)
//{id: 42, latitude: 12.97161923, longitude: 77.59463452, distance: 123},
case class Driver(id: Int, latitude: Double, longitude: Double, distance: Double)
case class SearchCriteria(latitude: Double, longitude: Double, radius: Int, limit: Int) {
  require(Math.abs(latitude) <= 90, "Latitude should be between +/- 90")
}

case class TotalDrivers(totalDrivers: Long)

object DriverJsonProtocol extends DefaultJsonProtocol {

  implicit val driverFormat = jsonFormat4(Driver)
  implicit val driverLocationFormat = jsonFormat4(DriverLocation)
  implicit val totalDriversFormat = jsonFormat1(TotalDrivers)
}

object DriverMongoProtocol {

  def driverLocationToDocument(driverLocation: DriverLocation): Document = {
    Document("driverId" -> driverLocation.driverId.get,
      "accuracy" -> driverLocation.accuracy,
      "location" -> Document("coordinates" -> List(driverLocation.longitude, driverLocation.latitude),
        "type" -> "Point"))
  }

  def searchCriteriaToDocument(searchCriteria: SearchCriteria): Document = {
    Document("$geoNear" -> Document(
      "maxDistance" -> searchCriteria.radius,
      "spherical" -> true,
      "distanceField" -> "distance",
      "limit" -> searchCriteria.limit,
      "near" -> Document("type" -> "Point",
        "coordinates" -> List(searchCriteria.longitude, searchCriteria.latitude))))
  }

  def documentToDriver(doc: Document): Driver= {
    import org.mongodb.scala.bson.BsonTransformer._
    val id = doc.getInteger("driverId")
    val distance = doc.getDouble("distance")
    val latLongValues = doc.get("location").map(x => {
      val bsonDoc = x.asDocument()
      bsonDoc.getArray("coordinates").getValues
    })
    val long = latLongValues.map(x => x.get(0).asDouble().getValue).getOrElse(0.0)
    val lat = latLongValues.map(x => x.get(1).asDouble().getValue).getOrElse(0.0)
    Driver(id, lat, long, distance)
  }

}
