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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.hbase.Cell;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ColumnIteratorTest {

	@Test
	public void test_firewalling() {
		// passing null collection
		ColumnIterator iterator = new ColumnIterator(null);
		Assert.assertFalse(iterator.hasNext());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void test_setOfBytes() {
		/*
		 * It is pointless to test the functionality of base iterator!  What we want to Assert.assert is that ColumnIterator delegates to the real iterators appropriately.
		 */
		Iterator<byte[]> iterator = mock(Iterator.class);
		// We want to make sure ColumnIterator will return exactly what the real iterator gives it.  Let's us doctor mock iteracor to return items in a particular order.
		final String[] values = new String[] {"a", "b", "c"};
		when(iterator.next()).thenAnswer(new Answer<byte[]>() {
			// return all the items of the values array in order as byte[].  After which return null.
			int index = 0;
			@Override
			public byte[] answer(InvocationOnMock invocation) throws Throwable {
				if (index < values.length) {
					return values[index++].getBytes(); // we need post increment
				} else {
					return null;
				}
			}
		});
		
		// We want hasNext() to return false after as many times as values were stuffed into it.
		when(iterator.hasNext()).thenAnswer(new Answer<Boolean>() {
			int i = 0;
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return i++ < values.length; // we want post increment
			}
		});
		
		// let's stuff this iterator into the collection that we would pass to the ColumnIterator
		Set<byte[]> collection = mock(Set.class);
		when(collection.iterator()).thenReturn(iterator);
		ColumnIterator columnIterator = new ColumnIterator(collection);
		int i = 0;
		while (columnIterator.hasNext()) {
			String value = columnIterator.next();
			Assert.assertEquals(values[i++], value);
		}
		// We should get back exactly as many items as were in the real iterator, no more no less
		Assert.assertEquals(3, i);

		// this should be called only once!
		verify(collection, times(1)).iterator();
		// verify next() was called on the iterator exactly 3 times
		verify(iterator, times(3)).next();
		
	}

	@SuppressWarnings("unchecked")
	@Test
	public void test_ListOfCell() {
		/*
		 * We are not interested in validating the behavior of the real iterator.  Instead just the behavior specific to the column iterator.
		 */
		final String[] qualifiers = new String[] {"a", "b", "c"};
		Iterator<Cell> iterator = mock(Iterator.class);
		// Have the iterator return true as many times as the size of keys array
		when(iterator.hasNext()).thenAnswer(new Answer<Boolean>() {
			int i = 0;
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return i++ < qualifiers.length;
			}
		});
		// have the iterator return a Cell composed of the key and value arrays
		when(iterator.next()).thenAnswer(new Answer<Cell>() {
			int i = 0;
			@Override
			public Cell answer(InvocationOnMock invocation)
					throws Throwable {
				Cell cell = mock(Cell.class);
				when(cell.getQualifierOffset()).thenReturn(0);
				when(cell.getQualifierLength()).thenReturn(1);
				when(cell.getQualifierArray()).thenReturn(qualifiers[i++].getBytes());
				return cell;
			}
		});
		// stuff it into the collection
		List<Cell> list = mock(List.class);
		when(list.iterator()).thenReturn(iterator);
		// now let's check the behavior
		ColumnIterator columnIterator = new ColumnIterator(list);
		int i = 0;
		while (columnIterator.hasNext()) {
			String value = columnIterator.next();
			Assert.assertEquals(qualifiers[i++], value);
		}
		// We should get back exactly as many items as were in the real iterator, no more no less
		Assert.assertEquals(3, i);

		// this should be called only once!
		verify(list, times(1)).iterator();
		// verify next() was called on the iterator exactly 3 times
		verify(iterator, times(3)).next();
		
	}
}
