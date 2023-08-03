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

public class ValidationFailureDetailsBuilder {
	protected String _fieldName;
	protected boolean _missing;
	protected boolean _semanticError;
	protected String _reason;
	protected String _subFieldName;
	protected boolean _internalError;
	protected int _errorCode;

	ValidationFailureDetailsBuilder becauseOf(String aReason) {
		_reason = aReason;
		return this;
	}
	
	ValidationFailureDetailsBuilder isMissing() {
		_missing = true;
		return this;
	}
	
	ValidationFailureDetailsBuilder isSemanticallyIncorrect() {
		_semanticError = true;
		return this;
	}
	
	ValidationFailureDetailsBuilder field(String fieldName) {
		_fieldName = fieldName;
		return this;
	}
	
	ValidationFailureDetails build() {
		return new ValidationFailureDetails(_errorCode, _fieldName, _subFieldName, _missing, _semanticError, _internalError, _reason);
	}

	ValidationFailureDetailsBuilder subField(String missingParameter) {
		_subFieldName = missingParameter;
		return this;
	}

	ValidationFailureDetailsBuilder isAnInternalError() {
		_internalError = true;
		return this;
	}

	ValidationFailureDetailsBuilder errorCode(int errorCode) {
		_errorCode = errorCode;
		return this;
	}
}
