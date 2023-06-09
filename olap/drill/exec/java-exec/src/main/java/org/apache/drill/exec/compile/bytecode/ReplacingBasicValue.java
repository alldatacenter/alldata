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
package org.apache.drill.exec.compile.bytecode;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;

import org.apache.drill.exec.util.AssertionUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * BasicValue with additional tracking information used to determine
 * the replaceability of the value (a holder, or boxed value) for scalar replacement purposes.
 *
 * <p>Contains a set of flags that indicate how the value is used. These
 * are updated throughout the life of the function, via the Analyzer/Interpreter,
 * which simulate execution of the function. After the analysis is complete, the
 * flags indicate how the value/variable was used, and that in turn indicates whether
 * or not we can replace it.
 */
public class ReplacingBasicValue extends BasicValue {
  private final ValueHolderIden iden; // identity of the holder this value represents
  private final int index; // the original local variable slot this value/holder was assigned

  /**
   * The set of flags associated with this value.
   *
   * <p>This is factored out so that it can be shared among several values
   * that are associated with each other.
   */
  private static class FlagSet {
    boolean isFunctionReturn = false;
    boolean isFunctionArgument = false;
    boolean isAssignedToMember = false;
    boolean isAssignedInConditionalBlock = false;
    boolean isThis = false;

    /**
     * Merge the given flag set into this one, creating a logical disjunction of the
     * values.
     *
     * @param other the flag set to merge into this one
     */
    public void mergeFrom(final FlagSet other) {
      if (other.isFunctionReturn) {
        isFunctionReturn = true;
      }
      if (other.isFunctionArgument) {
        isFunctionArgument = true;
      }
      if (other.isAssignedToMember) {
        isAssignedToMember = true;
      }
      if (other.isAssignedInConditionalBlock) {
        isAssignedInConditionalBlock = true;
      }
      if (other.isThis) {
        isThis = true;
      }
    }

    /**
     * Is the value with these flags replaceable?
     *
     * @return whether or not the value is replaceable
     */
    public boolean isReplaceable() {
      return !(isFunctionReturn || isFunctionArgument || isAssignedToMember || isAssignedInConditionalBlock || isThis);
    }

    /**
     * Dump the flag set's values to a given StringBuilder. The content is added to
     * a single line; no newlines are generated.
     *
     * <p>This is for logging and debugging purposes.
     *
     * @param sb the StringBuilder
     */
    public void dump(final StringBuilder sb) {
      boolean needSpace = false;

      if (isFunctionReturn) {
        sb.append("return");
        needSpace = true;
      }

      if (isFunctionArgument) {
        if (needSpace) {
          sb.append(' ');
        }

        sb.append("argument");
        needSpace = true;
      }

      if (isAssignedToMember) {
        if (needSpace) {
          sb.append(' ');
        }

        sb.append("member");
        needSpace = true;
      }

      if (isAssignedInConditionalBlock) {
        if (needSpace) {
          sb.append(' ');
        }

        sb.append("conditional");
        needSpace = true;
      }

      if (isThis) {
        if (needSpace) {
          sb.append(' ');
        }

        sb.append("this");
      }
    }
  }

  private FlagSet flagSet;
  /**
   * map of ReplacingBasicValue -> null; we need an IdentityHashSet, but there's no such thing;
   * we just always set the value in the map to be null, and only care about the keys.
   * Another solution might have been to define equals() and hashCode(), as apparently they are
   * defined by BasicValue, but it's not clear what to do with some of the additional members here.
   * It's not clear that not defining those won't also come back to bite us, depending on what ASM is
   * doing with BasicValues under the covers.
   *
   * This value is null until we have our first associate, at which point this is allocated on demand.
   */
  private IdentityHashMap<ReplacingBasicValue, ReplacingBasicValue> associates = null;
  // TODO remove?
  private HashSet<Integer> frameSlots = null; // slots in stack frame this has been assigned to

  /**
   * Create a new value representing a holder (boxed value).
   *
   * @param type the type of the holder
   * @param iden the ValueHolderIden for the holder
   * @param index the original local variable slot assigned to the value
   * @param valueList TODO
   * @return
   */
  public static ReplacingBasicValue create(final Type type, final ValueHolderIden iden, final int index,
      final List<ReplacingBasicValue> valueList) {
    final ReplacingBasicValue replacingValue = new ReplacingBasicValue(type, iden, index);
    valueList.add(replacingValue);
    return replacingValue;
  }

  /**
   * Add spaces to a StringBuilder.
   *
   * @param sb the StringBuilder
   * @param indentLevel the number of spaces to add
   */
  private static void indent(final StringBuilder sb, final int indentLevel) {
    for(int i = 0; i < indentLevel; ++i) {
      sb.append(' ');
    }
  }

  /**
   * Dump the value's members to a StringBuilder.
   *
   * <p>For logging and/or debugging.
   *
   * @param sb the StringBuilder
   * @param indentLevel the amount of indentation (in spaces) to prepend to each line
   */
  public void dump(final StringBuilder sb, final int indentLevel) {
    indent(sb, indentLevel);
    if (iden != null) {
      iden.dump(sb);
    } else {
      sb.append("iden is null");
    }
    sb.append('\n');
    indent(sb, indentLevel + 1);
    sb.append(" index: " + index);
    sb.append(' ');
    flagSet.dump(sb);
    sb.append(" frameSlots: ");
    dumpFrameSlots(sb);
    sb.append('\n');

    if (associates != null) {
      indent(sb, indentLevel);
      sb.append("associates(index)");
      for(ReplacingBasicValue value : associates.keySet()) {
        sb.append(' ');
        sb.append(value.index);
      }
      sb.append('\n');
    }
  }

  private ReplacingBasicValue(final Type type, final ValueHolderIden iden, final int index) {
    super(type);
    this.iden = iden;
    this.index = index;
    flagSet = new FlagSet();
  }

  /**
   * Indicates whether or not this value is replaceable.
   *
   * @return whether or not the value is replaceable
   */
  public boolean isReplaceable() {
    return flagSet.isReplaceable();
  }

  private void dumpFrameSlots(final StringBuilder sb) {
    if (frameSlots != null) {
      boolean first = true;
      for(Integer i : frameSlots) {
        if (!first) {
          sb.append(' ');
        }
        first = false;
        sb.append(i.intValue());
      }
    }
  }

  public void setFrameSlot(final int frameSlot) {
    if (frameSlots == null) {
      frameSlots = new HashSet<>();
    }
    frameSlots.add(frameSlot);
  }

  /**
   * Add another ReplacingBasicValue with no associates to this value's set of
   * associates.
   *
   * @param other the other value
   */
  private void addOther(final ReplacingBasicValue other) {
    assert other.associates == null;
    associates.put(other, null);
    other.associates = associates;
    flagSet.mergeFrom(other.flagSet);
    other.flagSet = flagSet;
  }

  /**
   * Associate this value with another.
   *
   * <p>This value and/or the other may each already have their own set of
   * associates.
   *
   * <p>Once associated, these values will share flag values, and a change to
   * any one of them will be shared with all other members of the association.
   *
   * @param other the other value
   */
  public void associate(final ReplacingBasicValue other) {
    associate0(other);

    if (AssertionUtil.ASSERT_ENABLED) {
      assert associates == other.associates;
      assert flagSet == other.flagSet;
      assert associates.containsKey(this);
      assert associates.containsKey(other);

      // check all the other values as well
      for(ReplacingBasicValue value : associates.keySet()) {
        assert associates.get(value) == null; // we never use the value
        assert value.associates == associates;
        assert value.flagSet == flagSet;
      }
    }
  }

  /**
   * Does the real work of associate(), which is a convenient place to
   * check all the assertions after this work is done.
   */
  private void associate0(final ReplacingBasicValue other) {
    // if it's the same value, there's nothing to do
    if (this == other) {
      return;
    }

    if (associates == null) {
      if (other.associates != null) {
        other.associate(this);
        return;
      }

      // we have no associations so far, so start collecting them
      associates = new IdentityHashMap<>();
      associates.put(this, null);
      addOther(other);
      return;
    }

    // if we got here, we have associates
    if (other.associates == null) {
      addOther(other);
      return;
    }

    // if we're already associated, there's nothing to do
    if (associates.containsKey(other)) {
      return;
    }

    // this and other both have disjoint associates; we need to merge them
    IdentityHashMap<ReplacingBasicValue, ReplacingBasicValue> largerSet = associates;
    FlagSet largerFlags = flagSet;
    IdentityHashMap<ReplacingBasicValue, ReplacingBasicValue> smallerSet = other.associates;
    FlagSet smallerFlags = other.flagSet;

    // if necessary, swap them, so the larger/smaller labels are correct
    if (largerSet.size() < smallerSet.size()) {
      final IdentityHashMap<ReplacingBasicValue, ReplacingBasicValue> tempSet = largerSet;
      largerSet = smallerSet;
      smallerSet = tempSet;
      final FlagSet tempFlags = largerFlags;
      largerFlags = smallerFlags;
      smallerFlags = tempFlags;
    }

    largerFlags.mergeFrom(smallerFlags);

    final int largerSize = largerSet.size();
    for(ReplacingBasicValue value : smallerSet.keySet()) {
      value.flagSet = largerFlags;
      value.associates = largerSet;
      largerSet.put(value, null);
    }

    assert associates == largerSet;
    assert largerSet.size() == largerSize + smallerSet.size();
    assert flagSet == largerFlags;
    assert largerSet.containsKey(this);
    assert other.associates == largerSet;
    assert other.flagSet == largerFlags;
    assert largerSet.containsKey(other);
  }

  /**
   * Mark this value as being used as a function return value.
   */
  public void markFunctionReturn() {
    flagSet.isFunctionReturn = true;
  }

  /**
   * Clear the indication that this value is used as a function return value.
   */
  public void disableFunctionReturn() {
    flagSet.isFunctionReturn = false;
  }

  /**
   * Indicates whether or not this value is used as a function return value.
   *
   * @return whether or not this value is used as a function return value
   */
  public boolean isFunctionReturn() {
    return flagSet.isFunctionReturn;
  }

  /**
   * Mark this value as being used as a function argument.
   */
  public void setFunctionArgument() {
    flagSet.isFunctionArgument = true;
  }

  /**
   * Indicates whether or not this value is used as a function argument.
   *
   * @return whether or not this value is used as a function argument
   */
  public boolean isFunctionArgument() {
    return flagSet.isFunctionArgument;
  }

  /**
   * Mark this value as being assigned to a class member variable.
   */
  public void setAssignedToMember() {
    flagSet.isAssignedToMember = true;
  }

  /**
   * Mark this value as being assigned to a variable inside of conditional block.
   */
  public void setAssignedInConditionalBlock() {
    flagSet.isAssignedInConditionalBlock = true;
  }

  /**
   * Indicates whether or not this value is assigned to a class member variable.
   *
   * @return whether or not this value is assigned to a class member variable
   */
  public boolean isAssignedToMember() {
    return flagSet.isAssignedToMember;
  }

  /**
   * Indicates whether or not this value is assigned to a variable inside of conditional block.
   *
   * @return whether or not this value is assigned to a variable inside of conditional block
   */
  public boolean isAssignedInConditionalBlock() {
    return flagSet.isAssignedInConditionalBlock;
  }

  /**
   * Return the ValueHolder identity for this value.
   *
   * @return the ValueHolderIden for this value
   */
  public ValueHolderIden getIden() {
    return iden;
  }

  /**
   * Get the original local variable slot assigned to this value/holder.
   *
   * @return the original local variable slot assigned to this value/holder
   */
  public int getIndex() {
    return index;
  }

  /**
   * Mark this value as a "this" reference.
   */
  public void setThis() {
    flagSet.isThis = true;
  }

  /**
   * Indicates whether or not this value is a "this" reference.
   *
   * @return whether or not this value is a "this" reference
   */
  public boolean isThis() {
    return flagSet.isThis;
  }
}
