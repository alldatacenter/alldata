/*
 * Copyright 2019 ABSA Group Limited
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

package za.co.absa.spline.harvester.exception

/**
 * All exceptions thrown by Harvester should inherit from this one
 *
 * @param message message describing the exception
 * @param throwable cause (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
 */
class HarvesterException(message: String, throwable: Throwable = null) extends Exception(message, throwable)
