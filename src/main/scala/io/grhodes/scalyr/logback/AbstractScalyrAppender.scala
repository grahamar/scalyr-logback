package io.grhodes.scalyr.logback

import java.util.concurrent.atomic.AtomicBoolean

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.core.{Layout, UnsynchronizedAppenderBase}
import com.scalyr.api.logs.{EventAttributes, Events}

private[logback] abstract class AbstractScalyrAppender[E]() extends UnsynchronizedAppenderBase[E] {

  private val DEFAULT_LAYOUT_PATTERN = "%d{\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\",UTC} %-5level [%thread] %logger: %m%n"
  private val layoutCreatedImplicitly = new AtomicBoolean(false)
  private var apiKey: String = ""
  private var serverHost: String = ""
  private var maxBufferRam: Long = 4194304
  private var logfile: String = "logback"
  private var parser: String = "logback"
  protected var layout: Layout[E] = _
  protected var pattern: String = _

  def getLogfile() = logfile
  def getParser() = parser
  def getApiKey() = apiKey
  def getServerHost() = serverHost.trim
  def getMaxBufferRam() = maxBufferRam
  def getPattern() = pattern
  def getLayout() = layout

  def setLogfile(logfile: String): Unit = {
    this.logfile = logfile
  }

  def setParser(parser: String): Unit = {
    this.parser = parser
  }

  def setApiKey(apiKey: String): Unit = {
    this.apiKey = apiKey
  }

  def setServerHost(serverHost: String): Unit = {
    this.serverHost = serverHost
  }

  def setMaxBufferRam(maxBufferRam: String): Unit = {
    Option(maxBufferRam).filter(_.nonEmpty).map(_.toLowerCase.trim).foreach {
      case mbr if mbr.contains("m") =>
        this.maxBufferRam = mbr.substring(0, mbr.indexOf("m")).toLong * 1048576
      case mbr if mbr.contains("k") =>
        this.maxBufferRam = mbr.substring(0, mbr.indexOf("k")).toLong * 1024
      case mbr => this.maxBufferRam = mbr.toLong
    }
  }

  def setPattern(pattern: String): Unit = {
    this.pattern = pattern
  }

  def setLayout(layout: Layout[E]): Unit = {
    this.layout = layout
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

  protected def createLayout(): Layout[E] = {
    val layout = new PatternLayout()
    layout.setPattern(Option(getPattern()).getOrElse(DEFAULT_LAYOUT_PATTERN))
    layout.asInstanceOf[Layout[E]]
  }

  final override def start(): Unit = {
    ensureLayout()
    if (!this.layout.isStarted) {
      this.layout.start()
    }

    val serverAttributes = new EventAttributes()
    if (getServerHost().length() > 0) {
      serverAttributes.put("serverHost", getServerHost())
    }
    serverAttributes.put("logfile", getLogfile())
    serverAttributes.put("parser", getParser())

    if(this.apiKey != null && !"".equals(this.apiKey.trim())) {
      // default to 4MB if not set.
      val maxBufferRam = Option(this.maxBufferRam).getOrElse(4194304L)
      Events.init(this.apiKey.trim(), maxBufferRam.toInt, null, serverAttributes)
      super.start()
    } else {
      addError("Cannot initialize logging. No Scalyr API Key has been set.")
    }
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

}
