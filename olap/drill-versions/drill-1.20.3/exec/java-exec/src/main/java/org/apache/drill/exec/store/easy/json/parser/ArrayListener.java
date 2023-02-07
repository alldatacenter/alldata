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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.easy.json.parser;

/**
 * Represents one level within array. The first time the parser sees the array element,
 * it will call the {@link #element(ValueDef)} method with the
 * look-ahead values visible to the parser. Since JSON is flexible, later
 * data shapes may not necessarily follow the first shape. The implementation
 * must handle this or throw an error if not supported.
 * <p>
 * When creating a multi-dimensional array, each array level is built one
 * by one. each will receive the same type information (decreased by one
 * array level.)
 * <p>
 * Then, while parsing, the parser calls events on the start and end of the
 * array, as well as on each element.
 * <p>
 * The array listener is an attribute of a value listener, represent the
 * "arrayness" of that value, if the value allows an array.
 *
 * <h4>Elements</h4>
 *
 * The array listener has a child listener that represents each element
 * in the array. The structure parser asks this listener to create that
 * child on the first element seen for the array. The structure parser
 * provides "look-ahead" type information for that element, when available.
 * <p>
 * Three JSON-specific cases warrant attention:
 * <ol>
 * <li>The first occurrence of the array is empty: {@code [ ]}. In this case,
 * the structure parser will defer asking for an element parser (and listener)
 * until an actual value appears. The array listener is responsible for
 * implementing some kind of "deferred type" logic to wait and see what
 * kind of element appears later.</li>
 * <li>The first occurrence of the array has, as its first element, a
 * {@code null} value. The structure parser will ask this listener to create
 * an array child for the {@code null} value, but the listener has no type
 * information. Since null values must be recorded (so we know how many
 * appear in each array), the listener is forced to choose a type. Choose
 * wisely as there is no way to know what type will appear in the future.</li>
 * <li>A generalized form of the above is that the structure parser only
 * knows what it sees on the first element when it asks for an element
 * child. In a well-formed file, that first token will predict the type
 * of all future tokens. But, JSON allows anything. The first element
 * might be {@code null}, an empty array, or a String. The second element
 * could be anything else (a number or an object). The listener, as always
 * is responsible for deciding how to handle type changes.</li>
 * </ol>
 *
 * <h4>Multi-Dimensional Arrays</h4>
 *
 * A multi-dimensional array is one of the form {@code [ [ ... }, that is,
 * the parser returns multiple levels of array start tokens. In this case,
 * listeners are structured as:
 * <ul>
 * <li>{@code ObjectListener} for the enclosing object which has a</li>
 * <li>{@code FieldListener} for the array value which has a</li>
 * <li>{@code ArrayListener} for the array, which has a</li>
 * <li>{@code ValueListener} for the elements. If the array is 1D,
 * the nesting stops here. But if it is 2+D, then the value has a</li>
 * <li>{@code ArrayListener} for the inner array, which has a</li>
 * <li>{@code ValueListener} for the elements. And so on recursively
 * for as many levels as needed or the array.</li>
 * </ul>
 */
public interface ArrayListener {

  /**
   * Called at the entrance to each level (dimension) of an array.
   * That is, called when the structure parser accepts the {@code [}
   * token.
   */
  void onStart();

  /**
   * Called for each element of the array. The array element is represented
   * by its own listener which receives the value of the element (if
   * scalar) or element events (if structured.)
   */
  void onElementStart();

  /**
   * Called after each element of the array.
   */
  void onElementEnd();

  /**
   * Called at the end of a set of values for an array. That is, called
   * when the structure parser accepts the {@code ]} token.
   */
  void onEnd();
}
