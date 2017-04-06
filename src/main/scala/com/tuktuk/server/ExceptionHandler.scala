package com.tuktuk.server

import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server._
import akka.http.javadsl.server.MissingHeaderRejection
import akka.http.scaladsl.model.{HttpMethod, StatusCodes}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import com.tuktuk.model._
import com.tuktuk.model.DriverJsonProtocol._

trait ExceptionHandlerTrait extends LogTracker {

  implicit val tuktukExceptionHandler = ExceptionHandler {
    //TODO: More granularity needed
    case ex: IllegalArgumentException => {
      warn("input validation failed", ex)
      complete(StatusCodes.BadRequest, Error(List(s"${ex.getMessage}")))
    }
    case ex: Exception => {
      error("Exception occured", ex)
      complete(StatusCodes.InternalServerError, Error(List(s"${ex.getMessage}")))
    }
  }
}

trait RejectionHandler extends LogTracker {

  implicit val rejectionHandler = RejectionHandler.newBuilder()
    .handleNotFound {
      complete(StatusCodes.NotFound, Error(List("No handle found")))
    }.handle {
      // Handle the response for input validation errors
      case ValidationRejection(msg, _) => {
        complete(StatusCodes.BadRequest, Error(List(s"${msg}")))
      }

      // Handle the response for invalid request entity
      case MalformedRequestContentRejection(msg, _) => {
        warn(s"Request Content validation failed : $msg")
        complete(StatusCodes.BadRequest, Error(List(s"${msg}")))
      }

      // Handle the response for missing request header
      case MissingHeaderRejection(header) => {
        warn(s"Missing request header : $header")
        complete(StatusCodes.BadRequest, Error(List(s"Missing request header : $header")))
      }

      // Handle the response for invalid request header
      case MalformedHeaderRejection(header, msg, _) => {
        warn(s"Request Header validation failed for $header : $msg")
        complete(StatusCodes.BadRequest, 
            Error(List(s"Request Header validation failed for $header : $msg")))
      }

      // Handle the error response when the mandatory query parameters are missing
      case MissingQueryParamRejection(parameter) => {
        warn(s"Missing request parameter : $parameter")
        complete(StatusCodes.BadRequest, 
            Error(List(s"Missing request parameter : $parameter")))
      }
      
      // Handle the error response when Http request method is not supported
      case MethodRejection(supportedMethod: HttpMethod) => {
        warn(s"Request method type validation failed : ${supportedMethod.name}")
        complete(StatusCodes.MethodNotAllowed, 
            Error(List(s"Request method type validation failed : ${supportedMethod.name}")))
      }

      // Handle the error response when request body entity expected but not supplied
      case RequestEntityExpectedRejection => {
        warn(s"Request  body  validation failed ")
        complete(StatusCodes.BadRequest, 
            Error(List(s"Request  body  validation failed")))
      }
     
    }
    .result()
}

trait CompositeDirective extends RejectionHandler with ExceptionHandlerTrait  {
  def compositeDirective: Directive0 = {
    val rejection = handleRejections(rejectionHandler)
    val exception = handleExceptions(tuktukExceptionHandler)
    rejection & exception
  }
}
