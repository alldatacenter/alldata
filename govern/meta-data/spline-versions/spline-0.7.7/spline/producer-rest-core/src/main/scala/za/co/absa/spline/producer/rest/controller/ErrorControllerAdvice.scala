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

package za.co.absa.spline.producer.rest.controller

import jakarta.validation.ValidationException
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.bind.annotation.{ControllerAdvice, ExceptionHandler}
import za.co.absa.commons.error.ErrorRef
import za.co.absa.spline.producer.service.InconsistentEntityException

@ControllerAdvice(basePackageClasses = Array(classOf[_package]))
class ErrorControllerAdvice {

  @ExceptionHandler(Array(
    classOf[NoSuchElementException]
  ))
  def notFound(e: Exception): ResponseEntity[_] = new ResponseEntity(e.getMessage, NOT_FOUND)

  @ExceptionHandler(Array(
    classOf[TypeMismatchException],
    classOf[HttpMessageConversionException],
    classOf[InconsistentEntityException],
    classOf[ValidationException],
  ))
  def badRequest(e: Exception): ResponseEntity[_] = new ResponseEntity(e.getMessage, BAD_REQUEST)

  @ExceptionHandler
  def serverError(e: Throwable): ResponseEntity[_] = new ResponseEntity(ErrorRef(e), INTERNAL_SERVER_ERROR)
}
