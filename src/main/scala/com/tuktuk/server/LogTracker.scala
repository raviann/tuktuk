package com.tuktuk.server

import scala.language.implicitConversions
import scala.concurrent.Future
import scala.util.{ Success, Failure, Try }
import org.slf4j.{ Logger, LoggerFactory }


trait LogTracker {


  private val log: Logger = LoggerFactory.getLogger(this.getClass);

  def trace(msg: => String): Unit = if (log.isTraceEnabled()) log.trace(msg)

  def debug(msg: => String): Unit = if (log.isDebugEnabled()) log.debug(msg)

  def info(msg: => String): Unit = if (log.isInfoEnabled()) log.info(msg)

  def warn(msg: => String): Unit = if (log.isWarnEnabled()) log.warn(msg)

  def error(msg: => String): Unit = {
    if (log.isErrorEnabled()) {
      log.error(msg)
    }
  }

  def trace(msg: => String, ex: Throwable): Unit = if (log.isTraceEnabled()) log.trace(msg, ex)

  def debug(msg: => String, ex: Throwable): Unit = if (log.isDebugEnabled()) log.debug(msg, ex)

  def info(msg: => String, ex: Throwable): Unit = if (log.isInfoEnabled()) log.info(msg, ex)

  def warn(msg: => String, ex: Throwable): Unit = if (log.isWarnEnabled()) log.warn(msg, ex)

  def error(msg: => String, ex: Throwable): Unit = {
    if (log.isErrorEnabled()) {
      log.error(msg, ex)
    }
  }
}
