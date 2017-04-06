package com.tuktuk.model

import spray.json._
import java.util.UUID
import scala.util.{ Try, Success, Failure }
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson._

/**
 * "latitude": 12.97161923, "longitude": 77.59463452, "accuracy": 0.7
 */
case class DriverLocation(driverId: Option[Int], latitude: Double, longitude: Double, accuracy: Double) {
  require(Math.abs(latitude) <= 90, "latitude should be between 90 to -90")
  require(Math.abs(longitude) <= 180, "longitude should be between 180 to -180")
  require(driverId.map(id => id>=1 && id<=5000).getOrElse(true),"driver id should be between 1 and 5000")
}
//{id: 42, latitude: 12.97161923, longitude: 77.59463452, distance: 123},
case class Driver(id: Int, latitude: Double, longitude: Double, distance: Double)
case class SearchCriteria(latitude: Double, longitude: Double, radius: Int, limit: Int) {
  require(Math.abs(latitude) <= 90, "Latitude should be between 90 to -90")
  require(Math.abs(longitude) <= 180, "Latitude should be between 180 to -180")
}

case class NearestDrivers(drivers:Option[Seq[Driver]])
case class TotalDrivers(totalDrivers: Long)

case class Error(errors:List[String])

object DriverJsonProtocol extends DefaultJsonProtocol {
  implicit val driverFormat = jsonFormat4(Driver)
  implicit val driverLocationFormat = jsonFormat4(DriverLocation)
  implicit val totalDriversFormat = jsonFormat1(TotalDrivers)
  implicit val nearestDriversFormat = jsonFormat1(NearestDrivers)
  implicit val errorFormat = jsonFormat1(Error)
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
