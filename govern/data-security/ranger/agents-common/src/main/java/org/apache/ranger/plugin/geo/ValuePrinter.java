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

package org.apache.ranger.plugin.geo;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

class ValuePrinter<T> implements ValueProcessor<T> {
	private static final Logger LOG = LoggerFactory.getLogger(ValuePrinter.class);

	private Writer writer;
	private String fileName;

	ValuePrinter(String fileName) {
		this.fileName = fileName;
	}

	public T process(T value) {

		if (value != null) {
			if (writer == null) {
				LOG.error("ValuePrinter.process() -" + value);
			} else {
				try {
					writer.write(value.toString());
					writer.write('\n');
				} catch (IOException exception) {
					LOG.error("ValuePrinter.process() - Cannot write '" + value + "' to " + fileName);
				}
			}
		}
		return value;
	}

	public void print(String str) {
		if (writer == null) {
			LOG.error("ValuePrinter.print() -" + str);
		} else {
			try {
				writer.write(str);
				writer.write('\n');
			} catch (IOException exception) {
				LOG.error("ValuePrinter.print() - Cannot write '" + str + "' to " + fileName );
			}
		}
	}

	void build() {
		try {
			if (StringUtils.isNotBlank(fileName)) {
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(fileName)));
			}
		} catch(IOException exception) {
			LOG.error("ValuePrinter.build() - Cannot open " + fileName + " for writing");
		}
	}

	void close() {
		try {
			if (writer != null) {
				writer.close();
			}
		} catch (IOException exception) {
			LOG.error("ValuePrinter.close() - Cannot close " + fileName);
		}
	}
}
