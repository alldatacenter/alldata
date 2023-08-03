/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.model.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ValidationFailureDetails {

	private static final Logger LOG = LoggerFactory.getLogger(ValidationFailureDetails.class);

	final String _fieldName;
	final String _subFieldName;
	final boolean _missing;
	final boolean _semanticError;
	final boolean _internalError;
	final String _reason;
	final int _errorCode;

	public ValidationFailureDetails(int errorCode, String fieldName, String subFieldName, boolean missing, boolean semanticError, boolean internalError, String reason) {
		_errorCode = errorCode;
		_missing = missing;
		_semanticError = semanticError;
		_internalError = internalError;
		_fieldName = fieldName;
		_subFieldName = subFieldName;
		_reason = reason;
	}

	public String getFieldName() {
		return _fieldName;
	}

	public boolean isMissingRequiredValue() {
		return _missing;
	}

	public boolean isSemanticallyIncorrect() {
		return _semanticError;
	}
	
	String getType() {
		if (_missing) return "missing";
		if (_semanticError) return "semantically incorrect";
		if (_internalError) return "internal error";
		return "";
	}

	public String getSubFieldName() {
		return _subFieldName;
	}

	@Override
	public String toString() {
		LOG.debug("ValidationFailureDetails.toString()");
		return String.format(" %s: error code[%d], reason[%s], field[%s], subfield[%s], type[%s]", "Validation failure",
				_errorCode, _reason, _fieldName, _subFieldName, getType());
	}

	@Override
	public int hashCode() {
		return Objects.hash(_fieldName, _subFieldName, _missing, _semanticError, _internalError, _reason, _errorCode);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ValidationFailureDetails)) {
			return false;
		}
		ValidationFailureDetails that = (ValidationFailureDetails)obj;
		return Objects.equals(_fieldName, that._fieldName) &&
				Objects.equals(_subFieldName, that._subFieldName) &&
				Objects.equals(_reason, that._reason) &&
				_internalError == that._internalError &&
				_missing == that._missing &&
				_semanticError == that._semanticError &&
				_errorCode == that._errorCode;
	}
}
