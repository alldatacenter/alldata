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
package org.apache.drill.exec.physical.resultSet.project;

/**
 * Plan-time properties of a requested column. Represents
 * a consolidated view of the set of references to a column.
 * For example, the project list might contain:
 * <ul>
 * <li>{@code SELECT *}</li>
 * <li>{@code SELECT filename, *, dir0}</li>
 * <li>{@code SELECT a, b, c}</li>
 * <li>{@code SELECT columns[4], columns[8]}</li>
 * <li>{@code SELECT a.b, a.c}</li>
 * <li>{@code SELECT columns, columns[1]}</li>
 * <li>{@code SELECT a, a.b}</li>
 * </ul>
 *
 * In each case, the same column is referenced in different
 * forms which are consolidated into this abstraction.
 * <p>
 * The resulting information is a "pattern": a form of reference.
 * Given the requested column, code can check if some concrete
 * reader-provided column is consistent with the requested
 * projection or not. The project
 * list does not contain sufficient information to definitively pick
 * a type; it only excludes certain types.
 * <p>
 * Even for complex types, we cannot definitively know the type.
 * For example, the projection {@code a[0]} could either refer to an
 * array (of any type), <b>or</b> a {@code DICT} with integer keys.
 * Similarly, a projection of the form {@code a.b} can either refer
 * to a member of a map, or the {@code "b"} string key of a
 * {@code DICT} column.
 *
 * <h4>Compatibility Rules</h4>
 *
 * The pattern given by projection is consistent with certain concrete types
 * as follows. + means any number of additional qualifiers. Note that the
 * following list is conceptual based on observed practice; the actual
 * implementation may be more restrictive.
 * <p>
 * <table>
 * <tr><th>Type</th><th>Consistent with</th></tr>
 * <tr><td>Non-repeated MAP</td>
 *     <td>{@code a}, {@code a.b}</td></tr>
 * <tr><td>Repeated MAP</td>
 *     <td>{@code a}, {@code a.b}, {@code a[n].b}</td></tr>
 * <tr><td>Non-repeated Scalar</td>
 *     <td>{@code a}</td></tr>
 * <tr><td>Repeated Scalar</td>
 *     <td>{@code a}, {@code a[n]}</td></tr>
 * <tr><td>Non-repeated DICT</td>
 *     <td>{@code a}, {@code a[n]}, {@code a['key']}</td></tr>
 * <tr><td>Repeated DICT</td>
 *     <td>{@code a}, {@code a[n]}, {@code a['key']}, {@code a[n][m]}, {@code a[n]['key']}</td></tr>
 * <tr><td>Non-repeated LIST</td>
 *     <td>{@code a}, {@code a[n]}</td></tr>
 * <tr><td>Repeated LIST</td>
 *     <td>{@code a}, {@code a[n]}, {@code a[n][n]}</td></tr>
 * </table>
 * <p>
 * MAP, DICT, UNION and LIST are structured types: projection can reach
 * into the structure to any number of levels. In such a case, when sufficient
 * schema information is available, the above rules can be applied recursively
 * to each level of structure. The recursion can be done in the class for a
 * DICT (since there is only one child type), but must be external for other
 * complex types. For MAP, the column can report which specific members
 * are projected.
 * <p>
 * The Text reader allows the {@code columns} column, which allows the
 * user to specify indexes. This class reports which indexes were actually
 * selected. Index information is available only at the top level, but
 * not for 2+ dimensions.
 */
public interface RequestedColumn {

  /**
   * The column name as projected. If the same column appears multiple
   * times (as in {@code a[1], A[2]}, then the case of the first appearance
   * is used.
   *
   * @return the column name as observed in the project list
   */
  String name();

  /**
   * Returns the fully-qualified column name. If the column is in the
   * top-level tuple, this is the same as {@code name()}. If the column
   * is nested in an array, then this name includes the enclosing
   * columns: {@code a.b.c}.
   *
   * @return the full name with enclosing map prefixes, if any
   */
  String fullName();

  /**
   * Case-insensitive comparison of the column name.
   */
  boolean nameEquals(String target);

  /**
   * Several consumers of this this mechanism process the "raw" projection list
   * which can contain a combination of wildcard and otehr columns. For example:
   * {@code filename, *, dir0}. The requested tuple preserves the wildcard
   * within the projection list so that, say, the projection mechanism can insert
   * the actual data columns between the two implicit columns in the example.
   * <p>
   * If a column is a wildcard, then none of the other methods apply, since
   * this projected column represents any number or actual columns.
   *
   * @return if this column is the wildcard placeholder
   */
  boolean isWildcard();

  /**
   * @return true if this column has no qualifiers. Example:
   * {@code a}.
   */
  boolean isSimple();

  /**
   * Report whether the projection implies a tuple. Example:
   * {@code a.b}. Not that this method, and others can only tell
   * if the projection implies a tuple; the actual column may
   * be a tuple (MAP), but be projected simply. The map
   * format also describes a DICT with a VARCHAR key.
   *
   * @return true if the column has a map-like projection.
   */
  boolean isTuple();

  /**
   * Return projection information for the column as a tuple. If
   * projection included references to nested columns (such as
   * {@code a.b, a.c}, then the tuple projection will list only
   * the referenced columns. However, if projection is generic
   * ({@code m}), then we presume all columns of the map are projected
   * and the returned object assumes all members are projected.
   *
   * @return projection information for a (presumed) map column
   */
  RequestedTuple tuple();

  /**
   * Report whether the first qualifier is an array.
   * Example: {@code a[1]}. The array format also describes
   * a DICT with an integer key.
   * @return true if the column must be an array.
   */
  boolean isArray();

  /**
   * If {@code isArray()} returns true, reports the number of dimensions
   * observed in projection. That is if projection is {@code a[0][1]},
   * then this method returns 2.
   * <p>
   * Note that, as with all projection-level information, this number
   * reflects only what was in the project list; not what might be
   * the number of dimensions in the actual input source.
   *
   * @return the maximum number of array dimensions observed in the
   * projection list, or 0 if this column was not observed to be an
   * array (if {@code isArray()} returns {@code false}.
   */
  int arrayDims();

  /**
   * Reports if the projection list included (only) specific element
   * indexes. For example: {@code a[2], a[5]}. The user could also project
   * both indexes and the array: {@code a[0], a}. In this case
   * {@code isArray()} is {code true}, but {@code hasIndexes()} is {@code false}.
   *
   * @return {@code true} if the column has enumerated indexes, {@code false}
   * if the column was also projected as a whole, or if this column
   * was not observed to be an array
   */
  boolean hasIndexes();

  /**
   * Return the maximum index value, if only explicit indexes were given.
   * Valid if {@code hasIndexes()} returns true.
   *
   * @return the maximum array index value known to the projection, or
   * 0 if {@code isArray()} is {@code false}. Also returns 0 if
   * {@code hasIndexe()} returns {@code false}, meaning that either
   * the column was not observed to be an array, or was projected
   * with both indexes and by itself: {@code a[0], a}.
   */
  int maxIndex();

  /**
    * Return a bitmap of the selected indexes. Only valid if
    * {@code hasIndexes()} returns {@code true}.
    * @return a bitmap of the selected array indexes, or {@code null}
    * if {@code hasIndexes()} returns {@code false}.
    */
  boolean[] indexes();

  /**
   * Report is a specific index was selected. Short cut for the other
   * array methods. Used in cases such as the {@code columns} column where
   * the user can select specific elements (column) but not others.
   *
   * @param index the array index to check
   * @return {@code true} if the array element was projected, either
   * explicitly ({@code a[3]}) or implicitly ({@code a}). Returns
   * {@code false} <i>only</i> if {@code hasIndexes()} returns
   * {@code true} (the user listed only explicit indexes) and the
   * requested index is not among those requested ({@code index >=
   * maxIndex() || !indexes()[index]})
   */
  boolean hasIndex(int index);

  /**
   * The internal qualifier information for the column. Generally not
   * needed by clients; use the other informations to interpret the
   * qualifier for you.
   *
   * @return detailed column qualifier information, if the column was
   * seen to be complex in the project list
   */
  Qualifier qualifier();
}
