/*
 * Copyright 2020 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.web.filter

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.lang.StringUtils.substringBefore
import za.co.absa.commons.config.ConfigurationImplicits.ConfigurationRequiredWrapper
import za.co.absa.spline.common.config.DefaultConfigurationStack
import za.co.absa.spline.common.io.OutputCapturingHttpResponseWrapper
import za.co.absa.spline.web.filter.SPAFilter._

import scala.collection.JavaConverters._


class SPAFilter extends Filter {

  private val uiConfigJson = {
    val webAppConf = new DefaultConfigurationStack
    val consumerUrl = webAppConf.getRequiredString(WebAppConfKey.ConsumerUrl)
    s"""{"${UIConfKey.ConsumerUrl}": "$consumerUrl"}"""
  }

  private var deploymentContextPlaceholder: String = _
  private var appBase: String = _
  private var indexPath: String = _
  private var assetsPath: String = _
  private var configPath: String = _

  override def destroy(): Unit = {}

  override def init(filterConfig: FilterConfig): Unit = {
    deploymentContextPlaceholder = filterConfig.getInitParameter(Param.BaseHrefPlaceholder)
    appBase = filterConfig.getInitParameter(Param.AppPrefix)
    indexPath = filterConfig.getInitParameter(Param.IndexPath)
    assetsPath = filterConfig.getInitParameter(Param.AssetsPath)
    configPath = filterConfig.getInitParameter(Param.ConfigPath)
  }

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val httpReq = request.asInstanceOf[HttpServletRequest]
    val httpRes = response.asInstanceOf[HttpServletResponse]

    if (httpReq.getServletPath == configPath) {
      httpRes.getWriter.write(uiConfigJson)
    } else if (isSPARouting(httpReq)) {
      handleSPARouting(httpReq, httpRes)
    } else {
      chain.doFilter(request, response)
    }
  }

  private def isSPARouting(req: HttpServletRequest) = {
    val reqPath = req.getServletPath
    (reqPath.startsWith(appBase)
      && !reqPath.startsWith(assetsPath)
      && isTextHtmlAccepted(req.getHeaders(Header.Accept).asScala.toSeq))
  }

  private def handleSPARouting(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    val responseWrapper = new OutputCapturingHttpResponseWrapper(res)
    val dispatcher = req.getRequestDispatcher(indexPath)

    dispatcher.include(req, responseWrapper)

    val rawHtml = responseWrapper.getContentAsString
    val fixedHtml = rawHtml.replace(deploymentContextPlaceholder, req.getContextPath)

    res.setHeader(Header.ContentType, MimeType.TextHtml)
    res.getWriter.write(fixedHtml)
  }
}

object SPAFilter {

  object WebAppConfKey {
    val ConsumerUrl = "spline.consumer.url"
  }

  object UIConfKey {
    val ConsumerUrl = "splineConsumerApiUrl"
  }

  object Header {
    val Accept = "Accept"
    val ContentType = "Content-Type"
  }

  object MimeType {
    val TextHtml = "text/html"
  }

  object Param {
    val BaseHrefPlaceholder = "deploymentContextPlaceholder"
    val AppPrefix = "appPrefix"
    val IndexPath = "indexPath"
    val AssetsPath = "assetsPath"
    val ConfigPath = "configPath"
  }

  private def isTextHtmlAccepted(acceptHeaderValues: Seq[String]) = {
    val acceptMimes = acceptHeaderValues.flatMap(parseAcceptHeader)
    acceptMimes.isEmpty || acceptMimes.contains(MimeType.TextHtml)
  }

  private def parseAcceptHeader(accept: String) =
    for {
      s <- accept.split("\\s*,\\s*")
      if s.nonEmpty
    } yield substringBefore(s, ";").toLowerCase
}
