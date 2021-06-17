package controllers

import javax.inject._

import akka.actor.ActorSystem
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import services.DictServices

import scala.concurrent.Future

trait Boot {
  def begin(): Unit

  def end(): Unit
}

/**
  * Created by wulinhao on 2017/8/16.
  */
@Singleton
class BootImpl @Inject()(dictServices: DictServices, appLifecycle: ApplicationLifecycle, config: Configuration, actorSystem: ActorSystem) extends Boot {
  val appLogger: Logger = LoggerFactory.getLogger("application")

  override def begin(): Unit = {
    //    hardorClient.init()
    dictServices.init()
  }

  override def end(): Unit = appLogger.info("end")

  // You can do this, or just explicitly call `hello()` at the end
  def start(): Unit = begin()

  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    end()
    Future.successful(())
  }

  // Called when this singleton is constructed (could be replaced by `hello()`)
  start()
}