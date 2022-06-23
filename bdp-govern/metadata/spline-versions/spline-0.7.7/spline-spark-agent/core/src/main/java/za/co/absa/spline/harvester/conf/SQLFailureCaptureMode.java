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

package za.co.absa.spline.harvester.conf;

public enum SQLFailureCaptureMode {

    // Do NOT capture any failed SQL executions
    NONE,

    // Only capture failed SQL executions when the error is non-fatal (see [[scala.util.control.NonFatal]])
    NON_FATAL,

    // Capture all failed SQL executions regardless of the error type
    ALL,
}
