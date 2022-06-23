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


object RedirectFilter {

  object Param {
    val Location = "location"
  }

}

class RedirectFilter extends Filter {
  private var location: String = _

  override def init(config: FilterConfig): Unit = {
    location = config.getInitParameter(RedirectFilter.Param.Location)
  }

  override def doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain): Unit = {
    val contextPath = req.asInstanceOf[HttpServletRequest].getContextPath
    resp.asInstanceOf[HttpServletResponse].sendRedirect(s"$contextPath$location")
  }

  override def destroy(): Unit = {}
}
