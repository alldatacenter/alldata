import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._
import javax.inject.Singleton

import org.slf4j.LoggerFactory
import play.api.libs.json.Json

@Singleton
class ErrorHandler extends HttpErrorHandler {
  val logger = LoggerFactory.getLogger("application")

  case class ReError(isSuccess: Boolean = false, msg: String)

  implicit val ReErrorFormat = Json.format[ReError]

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Future.successful(
      Status(statusCode)("A client error occurred: " + message)
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    logger.error(s"query ${request.uri} catch exception", exception)
    Future.successful(
      Status(200)(Json.toJson(ReError(msg = s"A server error occurred ${exception.getMessage}")))
    )
  }
}