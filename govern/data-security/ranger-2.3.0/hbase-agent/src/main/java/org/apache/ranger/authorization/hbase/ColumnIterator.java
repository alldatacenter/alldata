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
package org.apache.ranger.authorization.hbase;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColumnIterator implements Iterator<String> {
	// TODO write tests for this class
	
	private static final Logger LOG = LoggerFactory.getLogger(ColumnIterator.class.getName());
	Iterator<byte[]> _setIterator;
	Iterator<Cell> _listIterator;
	
	@SuppressWarnings("unchecked")
	public ColumnIterator(Collection<?> columnCollection) {
		if (columnCollection != null) {
			if (columnCollection instanceof Set) {
				_setIterator = ((Set<byte[]>)columnCollection).iterator();
			} else if (columnCollection instanceof List) {
				_listIterator = ((List<Cell>)columnCollection).iterator();
			} else { // unexpected
				// TODO make message better
				LOG.error("Unexpected type " + columnCollection.getClass().getName() + " passed as value in column family collection");
			}
		}
	}

	@Override
	public boolean hasNext() {
		if (_setIterator != null) {
			return _setIterator.hasNext();
		} else if (_listIterator != null) {
			return _listIterator.hasNext();
		} else {
			return false;
		}
	}

	/**
	 * Never returns a null value.  Will return empty string in case of null value.
	 */
	@Override
	public String next() {
		String value = "";
		if (_setIterator != null) {
			byte[] valueBytes = _setIterator.next();
			if (valueBytes != null) {
				value = Bytes.toString(valueBytes);
			}
		} else if (_listIterator != null) {
			Cell cell = _listIterator.next();
			byte[] v = CellUtil.cloneQualifier(cell);
			if (v != null) {
				value = Bytes.toString(v);
			}
		} else {
			// TODO make the error message better
			throw new NoSuchElementException("Empty values passed in!");
		}
		return value;
	}

	@Override
	public void remove() {
		// TODO make the error message better
		throw new UnsupportedOperationException("Remove not supported from iterator!");
	}

}
