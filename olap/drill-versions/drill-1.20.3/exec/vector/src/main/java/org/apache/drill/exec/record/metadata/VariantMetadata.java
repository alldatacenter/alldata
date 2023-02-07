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
package org.apache.drill.exec.record.metadata;

import java.util.Collection;

import org.apache.drill.common.types.TypeProtos.MinorType;

/**
 * Describes the contents of a list or union field. Such fields are,
 * in effect, a map from minor type to vector, represented here as
 * a map from minor type to column metadata. The child columns used here
 * are a useful fiction. The column name is made up to be the same as
 * the name of the type.
 * <p>
 * In Drill, a union and a list are related, but distinct. In metadata,
 * a union is an optional variant while a list is a variant array.
 * This makes the representation simpler
 * and should be a good-enough approximation of reality.
 * <p>
 * Variants can contain three kinds of children:
 * <ul>
 * <li>Nullable (optional) scalar vectors.</li>
 * <li>Non-nullable (required) map.</li>
 * <li>Nullable (optional) list.</li>
 * </ul>
 * <p>
 * A union cannot contain a repeated vector. Instead, the
 * union can contain a list. Note also that maps can never be optional,
 * so they are required in the union, even though the map is, in effect,
 * optional (the map is effectively null if it is not used for a give
 * row.) Yes, this is confusing, but it is how the vectors are
 * implemented (for now.)
 * <p>
 * A list type is modeled here as a repeated union type. This is not
 * entirely accurate, but it is another useful fiction. (In actual
 * implementation, a list is either a single type, or a array of
 * unions. This detail is abstracted away here.)
 * <p>
 * In vector implementation, unions declare their member types, but
 * lists don't. Here, both types declare their member types. (Another
 * useful fiction.)
 * <p>
 * A union or list can contain a map. Maps have structure. To support this,
 * the metadata allows adding a map column that contains the map structure.
 * Such metadata exist only in this system; it is not easily accessible in
 * the vector implementation.
 * <p>
 * A union or list can contain a list (though not a union.) As described
 * here, lists can have structure, and so, like maps, can be built using
 * a column that provides that structure.
 * <p>
 * Note that the Drill {@link MinorType#UNION UNION} and
 * {@link MinorType#LIST LIST} implementations are considered experimental
 * and are not generally enabled. As a result, this metadata schema must
 * also be considered experimental and subject to change.
 */

public interface VariantMetadata {

  /**
   * Add any supported type to the variant.
   * <p>
   * At present, the union
   * vector does not support the decimal types. This class does not
   * reject such types; but they will cause a runtime exception when
   * code asks the union vector for these types.
   *
   * @param type type to add
   * @return the "virtual" column for that type
   * @throws IllegalArgumentException if the type has already been
   * added
   */

  ColumnMetadata addType(MinorType type);

  /**
   * Add a column for any supported type to the variant.
   * Use this to add structure to a list or map member.
   *
   * @param col column to add. The column must have the correct
   * mode. The column's type is used as the type key
   * @throws IllegalArgumentException if the type has already been
   * added, or if the mode is wrong
   */

  void addType(ColumnMetadata col);

  /**
   * Returns the number of types in the variant.
   *
   * @return the number of types in the variant
   */

  int size();

  /**
   * Determine if the given type is a member of the variant.
   *
   * @param type type to check
   * @return <tt>true</tt> if the type is a member,
   * <tt>false</tt> if not
   */

  boolean hasType(MinorType type);

  /**
   * Returns the list of types which are members of this variant.
   *
   * @return the list of types
   */

  Collection<MinorType> types();

  Collection<ColumnMetadata> members();

  /**
   * Retrieve the virtual column for a given type.
   *
   * @param type the type key
   * @return the virtual column, or <tt>null</tt> if the type
   * is not a member of the variant
   */

  ColumnMetadata member(MinorType type);

  /**
   * Return the column that defines this variant structure
   *
   * @return the column that returns this variant structure
   * from its {@link ColumnMetadata#variantSchema() variantSchema()}
   * method
   */

  ColumnMetadata parent();

  /**
   * A list is defined as a list of variants at the metadata layer.
   * But, in implementation, a list will do special processing if the
   * variant (union) contains only one type.
   *
   * @return <tt>true</tt> if this variant contains only one type,
   * </tt>false</tt> if the variant contains 0, 2 or more types
   */

  boolean isSingleType();

  /**
   * Lists are odd creatures: they contain a union if they have more
   * than one subtype, but are like a nullable repeated type if they
   * contain only one type. This method returns the type of the array:
   * either the single type (if {@link #isSingleType()} is <tt>true</tt>)
   * or a reference to the synthetic union column nested inside the
   * list.
   * @return the metadata for the implicit column within the list
   */

  ColumnMetadata listSubtype();

  void becomeSimple();
  boolean isSimple();
}
