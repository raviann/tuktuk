package com.tuktuk

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import com.tuktuk.server.WebServer;

import scala.util.{Failure, Success}
import akka.actor.Props
import com.tuktuk.model._
import com.tuktuk.GlobalActorSystem
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.collection.immutable.Document
import com.mongodb.connection.ClusterSettings
import org.mongodb.scala.ServerAddress
import org.mongodb.scala.MongoClientSettings

object Boot extends App with WebServer {

  implicit val system = GlobalActorSystem.actorSystem
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher
  
  val mongoClient: MongoClient = MongoClient()
  
  val database: MongoDatabase = mongoClient.getDatabase("local");
  println(s"""name: ${database.name} - ${database.listCollectionNames()}""")
  
  database.createCollection("drivers");
  val driversCollection:MongoCollection[Document] = database.getCollection("drivers")
  driversCollection.createIndex(Document("location"-> "2dsphere"))
  
  
  val bindingFuture = Http().bindAndHandle(route(driversCollection), "localhost", 8080)
  

  bindingFuture.onComplete {
    case Success(binding) ⇒
      println(s"Server is listening on localhost:8080")
    case Failure(e) ⇒
      println(s"Binding failed with ${e.getMessage}")
      system.terminate()
  }

}