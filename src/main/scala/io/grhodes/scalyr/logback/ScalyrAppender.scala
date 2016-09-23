package io.grhodes.scalyr.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import com.scalyr.api.logs.{EventAttributes, Events}

class ScalyrAppender extends AbstractScalyrAppender[ILoggingEvent] {
  override def append(event: ILoggingEvent): Unit = {
    val msg = this.layout.doLayout(event)
    val level = event.getLevel.toInt
    if (level >= Level.ERROR_INT) {
      Events.error(new EventAttributes("message", msg))
    } else if (level >= Level.WARN_INT) {
      Events.warning(new EventAttributes("message", msg))
    } else if (level >= Level.INFO_INT) {
      Events.info(new EventAttributes("message", msg))
    } else if (level >= Level.DEBUG_INT) {
      Events.fine(new EventAttributes("message", msg))
    } else if (level >= Level.TRACE_INT) {
      Events.finer(new EventAttributes("message", msg))
    } else {
      Events.finest(new EventAttributes("message", msg))
    }
  }
}
