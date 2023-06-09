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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

class ValueHolderIden {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ValueHolderIden.class);

  // the index of a field is the number in which it appears within the holder
  private final ObjectIntHashMap<String> fieldMap; // field name -> index
  private final Type[] types; // the type of each field in the holder, by index
  private final String[] names; // the name of each field in the holder, by index
  private final int[] offsets; // the offset of each field in the holder, by index
  private final Type type; // type of the holder itself

  public ValueHolderIden(Class<?> c) {
    Field[] fields = c.getFields();

    // Find the non-static member variables
    List<Field> fldList = Lists.newArrayList();
    for (Field f : fields) {
      if (!Modifier.isStatic(f.getModifiers())) {
        fldList.add(f);
      }
    }

    this.type = Type.getType(c);

    this.types = new Type[fldList.size()];
    this.names = new String[fldList.size()];
    this.offsets = new int[fldList.size()];
    fieldMap = new ObjectIntHashMap<String>(fldList.size());
    int i = 0; // index of the next holder member variable
    int offset = 0; // offset of the next holder member variable
    for (Field f : fldList) {
      types[i] = Type.getType(f.getType());
      names[i] = f.getName();
      offsets[i] = offset;
      fieldMap.put(f.getName(), i);

      // the next offset and index
      offset += types[i].getSize();
      i++;
    }
  }

  public void dump(final StringBuilder sb) {
    sb.append("ValueHolderIden: type=" + type + '\n');
    for (ObjectIntCursor<String> oic : fieldMap) {
      sb.append("  " + oic.key + ": i=" + oic.value + ", type=" + types[oic.value] +
          ", offset=" + offsets[oic.value] + '\n');
    }
  }

  private static void initType(int offset, Type t, DirectSorter v) {
    switch(t.getSort()) {
    case Type.BOOLEAN:
    case Type.BYTE:
    case Type.CHAR:
    case Type.SHORT:
    case Type.INT:
      v.visitInsn(Opcodes.ICONST_0);
      v.directVarInsn(Opcodes.ISTORE, offset);
      break;
    case Type.LONG:
      v.visitInsn(Opcodes.LCONST_0);
      v.directVarInsn(Opcodes.LSTORE, offset);
      break;
    case Type.FLOAT:
      v.visitInsn(Opcodes.FCONST_0);
      v.directVarInsn(Opcodes.FSTORE, offset);
      break;
    case Type.DOUBLE:
      v.visitInsn(Opcodes.DCONST_0);
      v.directVarInsn(Opcodes.DSTORE, offset);
      break;
    case Type.OBJECT:
      v.visitInsn(Opcodes.ACONST_NULL);
      v.directVarInsn(Opcodes.ASTORE, offset);
      break;
    default:
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Add the locals necessary to replace the members of a holder of this type.
   *
   * @param adder the method visitor to use to add the necessary instructions
   * @param defaultFirst the default index to return for the first variable
   *   if we don't find another one
   * @return the index of the first local variable (standing in for the first holder member)
   */
  private int addLocals(final DirectSorter adder, final int defaultFirst) {
    int first = defaultFirst;
    for (int i = 0; i < types.length; i++) {
      int varIndex = adder.newLocal(types[i]);
      if (i == 0) {
        first = varIndex; // first offset
      }
    }

    return first;
  }

  public ValueHolderSub getHolderSub(DirectSorter adder) {
    final int first = addLocals(adder, -1);
    return new ValueHolderSub(first);
  }

  public ValueHolderSub getHolderSubWithDefinedLocals(int first) {
    return new ValueHolderSub(first);
  }

  /**
   * Return the DUP or DUP2 opcode appropriate for the given type.
   *
   * @param type the type
   * @return the DUP/DUP2 opcode to use for type
   */
  private static int getDupOpcode(final Type type) {
    assert type.getSize() >= 1;
    return type.getSize() == 1 ? Opcodes.DUP : Opcodes.DUP2;
  }

  /**
   * Transfer the member variables of this holder to local variables.
   *
   * <p>If this is used, the maximum stack size must be increased by one
   * to accommodate the extra DUP instruction this will generate.
   *
   * @param adder a visitor that will be called to add the necessary instructions
   * @param localVariable the offset of the first local variable to use
   */
  public void transferToLocal(final DirectSorter adder, final int localVariable) {
    for (int i = 0; i < types.length; i++) {
      final Type t = types[i];
      if (i + 1 < types.length) {
        adder.visitInsn(Opcodes.DUP); // not size dependent: always the objectref from which we'll GETFIELD
        }
      adder.visitFieldInsn(Opcodes.GETFIELD, type.getInternalName(), names[i], t.getDescriptor());
      adder.directVarInsn(t.getOpcode(Opcodes.ISTORE), localVariable + offsets[i]);
    }
  }

  /**
   * Create local variables and transfer the members of a holder to them.
   *
   * @param adder the method visitor to use to add the variables
   * @return the index of the first variable added
   */
  public int createLocalAndTransfer(final DirectSorter adder) {
    final int first = addLocals(adder, 0);
    transferToLocal(adder, first);
    return first;
  }

  public class ValueHolderSub {
    private int first; // TODO: deal with transfer() so this can be made final

    @Override
    public String toString() {
      return "ValueHolderSub(" + first + ")";
    }

    public ValueHolderSub(int first) {
      assert first != -1 : "Create Holder for sub that doesn't have any fields.";
      this.first = first;
    }

    public ValueHolderIden iden() {
      return ValueHolderIden.this;
    }

    public void init(DirectSorter mv) {
      for (int i = 0; i < types.length; i++) {
        initType(first + offsets[i], types[i], mv);
      }
    }

    public int first() {
      return first;
    }

    private int getFieldIndex(final String name, final InstructionModifier mv) {
      if (!fieldMap.containsKey(name)) {
        throw new IllegalArgumentException(String.format(
            "Unknown name '%s' on line %d.", name, mv.getLastLineNumber()));
      }
      return fieldMap.get(name); // using lget() is not thread-safe
    }

    public void addInsn(String name, InstructionModifier mv, int opcode) {
      switch (opcode) {
      case Opcodes.GETFIELD:
        addKnownInsn(name, mv, Opcodes.ILOAD);
        return;

      case Opcodes.PUTFIELD:
        addKnownInsn(name, mv, Opcodes.ISTORE);
      }
    }

    // TODO: do we really need this? Instead of moving the variables, can't we just
    // use the original locations in the subsequent references?
    public void transfer(InstructionModifier mv, int newStart) {
      // if the new location is the same as the current position, there's nothing to do
      if (first == newStart) {
        return;
      }

      for (int i = 0; i < types.length; i++) {
        mv.directVarInsn(types[i].getOpcode(Opcodes.ILOAD), first + offsets[i]);
        mv.directVarInsn(types[i].getOpcode(Opcodes.ISTORE), newStart + offsets[i]);
      }

      this.first = newStart;
    }

    private void addKnownInsn(String name, InstructionModifier mv, int analogOpcode) {
      int f = getFieldIndex(name, mv);
      Type t = types[f];
      mv.directVarInsn(t.getOpcode(analogOpcode), first + offsets[f]);
    }

    /**
     *
     * @param adder
     * @param owner
     * @param name
     * @param desc
     * @return amount of additional stack space that will be required for this instruction stream
     */
    public int transferToExternal(final DirectSorter adder, final String owner,
        final String name, final String desc) {
      // create a new object and assign it to the desired field.
      adder.visitTypeInsn(Opcodes.NEW, type.getInternalName());
      adder.visitInsn(getDupOpcode(type));
      adder.visitMethodInsn(Opcodes.INVOKESPECIAL, type.getInternalName(), "<init>", "()V", false);

      // now we need to set all of the values of this new object.
      int additionalStack = 0;
      for (int i = 0; i < types.length; i++) {
        final Type t = types[i];

        // dup the object where we are putting the field.
        adder.visitInsn(getDupOpcode(type)); // dup for every as we want to save in place at end.
        adder.directVarInsn(t.getOpcode(Opcodes.ILOAD), first + offsets[i]);
        adder.visitFieldInsn(Opcodes.PUTFIELD, type.getInternalName(), names[i], t.getDescriptor());

        /*
         * The above substitutes a reference to a scalar in a holder with a direct reference to
         * the scalar.
         *
         * In the case of longs or doubles, this requires more stack space than was used before;
         * if we were moving a reference to a holder with a long, we were previously moving the
         * reference. But now we're moving the long, which is twice as big. So we may need more
         * stack space than has currently been allocated.
         */
        if (t.getSize() > additionalStack) {
          additionalStack = t.getSize();
        }
      }

      // lastly we save it to the desired field.
      adder.visitFieldInsn(Opcodes.PUTFIELD, owner, name, desc);

      return additionalStack;
    }
  }
}
