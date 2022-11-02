/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.utils.ambari;


import org.apache.commons.validator.routines.RegexValidator;
import org.apache.commons.validator.routines.UrlValidator;

public class ValidatorUtils {
  /**
   * Validates filesystem URL
   * @param webhdfsUrl url
   * @return is url valid
   */
  public static boolean validateHdfsURL(String webhdfsUrl) {
    String[] schemes = {"webhdfs", "hdfs", "s3", "wasb", "swebhdfs", "adl"};
    return validateURL(webhdfsUrl, schemes);
  }

  public static boolean validateHttpURL(String webhdfsUrl) {
    String[] schemes = {"http", "https"};
    return validateURL(webhdfsUrl, schemes);
  }

  public static boolean validateURL(String webhdfsUrl, String[] schemes) {
    RegexValidator authority = new RegexValidator(".*");
    UrlValidator urlValidator = new UrlValidator(schemes, authority, UrlValidator.ALLOW_LOCAL_URLS);
    return urlValidator.isValid(webhdfsUrl);
  }
}
