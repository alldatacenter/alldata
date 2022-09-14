/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.exception

import org.apache.linkis.common.exception.ErrorException

/**
 * Basic job launch exception
 * @param errorCode error code
 * @param errorMsg error message
 */
class FlinkJobLaunchErrorException(errorCode: Int, errorMsg: String, t: Throwable) extends ErrorException(errorCode, errorMsg){
  this.initCause(t)
}

/**
 * Exception in triggering savepoint
 */
class FlinkSavePointException(errorCode: Int, errorMsg: String, t: Throwable)
  extends FlinkJobLaunchErrorException(errorCode, errorMsg, t)

/**
 * Exception in fetching job state
 */
class FlinkJobStateFetchException(errorCode: Int, errorMsg: String, t: Throwable)
  extends FlinkJobLaunchErrorException(errorCode, errorMsg, t)

class FlinkJobLogFetchException(errorCode: Int,  errorMsg: String, t: Throwable)
  extends FlinkJobLaunchErrorException(errorCode, errorMsg, t)

