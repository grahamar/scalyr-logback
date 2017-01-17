package io.grhodes.scalyr.logback

import java.util.concurrent.atomic.AtomicBoolean

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.core.{Layout, UnsynchronizedAppenderBase}
import com.scalyr.api.logs.{EventAttributes, Events}

import scala.collection.mutable.HashMap

private[logback] abstract class AbstractScalyrAppender[E]() extends UnsynchronizedAppenderBase[E] {

  private val DEFAULT_LAYOUT_PATTERN = "%d{\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\",UTC} %-5level [%thread] %logger: %m%n"
  private val layoutCreatedImplicitly = new AtomicBoolean(false)
  private val serverAttributes: HashMap[String, String] = new HashMap[String, String]()
  protected var layout: Layout[E] = _
  protected var pattern: String = _
  private var apiKey: String = ""
  private var serverHost: String = ""
  private var maxBufferRam: Long = 4194304
  private var logfile: String = "logback"
  private var parser: String = "logback"

  final override def start(): Unit = {
    ensureLayout()
    if (!this.layout.isStarted) {
      this.layout.start()
    }

    val eventAttributes = new EventAttributes()
    if (getServerHost().length() > 0) {
      eventAttributes.put("serverHost", getServerHost())
    }
    eventAttributes.put("logfile", getLogfile())
    eventAttributes.put("parser", getParser())

    this.serverAttributes.foreach {
      case (key, value) => eventAttributes.put(key, value)
    }

    if (this.apiKey != null && !"".equals(this.apiKey.trim())) {
      // default to 4MB if not set.
      val maxBufferRam = Option(this.maxBufferRam).getOrElse(4194304L)
      Events.init(this.apiKey.trim(), maxBufferRam.toInt, null, eventAttributes)
      super.start()
    } else {
      addError("Cannot initialize logging. No Scalyr API Key has been set.")
    }
  }

  def getLogfile() = logfile

  def setLogfile(logfile: String): Unit = {
    this.logfile = logfile
  }

  def getParser() = parser

  def setParser(parser: String): Unit = {
    this.parser = parser
  }

  def getServerHost() = serverHost.trim

  def setServerHost(serverHost: String): Unit = {
    this.serverHost = serverHost
  }

  protected final def ensureLayout() = {
    Option(getLayout()).orElse {
      layoutCreatedImplicitly.set(true)
      this.layout = createLayout()
      Option(layout)
    }.foreach { _ =>
      Option(this.layout.getContext).getOrElse {
        this.layout.setContext(getContext)
      }
    }
  }

  def getLayout() = layout

  def setLayout(layout: Layout[E]): Unit = {
    this.layout = layout
  }

  protected def createLayout(): Layout[E] = {
    val layout = new PatternLayout()
    layout.setPattern(Option(getPattern()).getOrElse(DEFAULT_LAYOUT_PATTERN))
    layout.asInstanceOf[Layout[E]]
  }

  def getPattern() = pattern

  def setPattern(pattern: String): Unit = {
    this.pattern = pattern
  }

  final override def stop(): Unit = {
    Events.flush()
    super.stop()
    if (this.layoutCreatedImplicitly.get()) {
      try {
        this.layout.stop()
      } finally {
        this.layout = null
        this.layoutCreatedImplicitly.set(false)
      }
    }
  }

  def getApiKey() = apiKey

  def setApiKey(apiKey: String): Unit = {
    this.apiKey = apiKey
  }

  def getMaxBufferRam() = maxBufferRam

  def setMaxBufferRam(maxBufferRam: String): Unit = {
    Option(maxBufferRam).filter(_.nonEmpty).map(_.toLowerCase.trim).foreach {
      case mbr if mbr.contains("m") =>
        this.maxBufferRam = mbr.substring(0, mbr.indexOf("m")).toLong * 1048576
      case mbr if mbr.contains("k") =>
        this.maxBufferRam = mbr.substring(0, mbr.indexOf("k")).toLong * 1024
      case mbr => this.maxBufferRam = mbr.toLong
    }
  }

  def getServerAttributes() = serverAttributes

  def addServerAttributes(keyValue: String): Unit = {
    val split = keyValue.split("=", 2)
    if (split.length == 2) this.serverAttributes(split(0)) = split(1)
  }

}
