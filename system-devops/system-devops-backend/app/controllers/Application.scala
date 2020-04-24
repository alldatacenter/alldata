package controllers

import javax.inject.{Inject, Singleton}
import org.slf4j.{Logger, LoggerFactory}
import utils.Sign

class ApiAction @Inject()(parser: BodyParsers.Default, config: Configuration)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
  val HEADER_KEY = "apiToken"
  val apiUser = "apiuser"
  val privateKey = config.get[String]("api.key")
  val auth = config.get[Boolean]("api.auth")


  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    if (auth) {
      if (checkApiAuth(request)) {
        block(request)
      }
      else {
        val j = Json.obj(
          "isSuccess" -> false,
          "msg" -> "no api auth infomation"
        )
        Future.successful(Forbidden(j.toString()))
      }
    }
    else {
      block(request)
    }
  }

  def checkApiAuth[A](request: Request[A]): Boolean = {
    try {
      val apiToken = request.headers.get(HEADER_KEY)
      apiToken.isDefined && Sign.checkSign(apiToken.get, privateKey)
    }
    catch {
      case e: Exception => false
    }
  }
}

@Singleton
class Application @Inject()(ws: WSClient, environment: Environment,
                            config: Configuration, cc: ControllerComponents) extends AbstractController(cc) {
  val duration = Duration("5000 sec")
  //  val testInt = config.get[Int]("test.int")

  val appLogger: Logger = LoggerFactory.getLogger("application")

  def redirectDocs = Action {
    Redirect(url = "/assets/lib/swagger-ui/index.html", queryString = Map("url" -> Seq("/swagger.json")))
  }

  def health = Action {
    Ok("{}")
  }
}