/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.common.security

import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, X509TrustManager}

object TLSUtils {

  lazy val TrustingAllSSLContext: SSLContext = {
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, Array(TrustAll), null)
    sslContext
  }

  // Bypasses both client and server validation.
  private object TrustAll extends X509TrustManager {
    override val getAcceptedIssuers: Array[X509Certificate] = Array.empty

    override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {
      // do nothing
    }

    override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {
      // do nothing
    }
  }

}
