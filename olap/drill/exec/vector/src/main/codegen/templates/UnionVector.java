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
import org.apache.drill.common.types.Types;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.vector.ValueVector;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/drill/exec/vector/complex/UnionVector.java" />


<#include "/@includes/license.ftl" />

package org.apache.drill.exec.vector.complex;

<#include "/@includes/vv_imports.ftl" />
import java.util.Iterator;
import java.util.Set;

import org.apache.drill.exec.vector.complex.impl.ComplexCopier;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.expr.BasicTypeHelper;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.record.MaterializedField;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")


/**
 * A vector which can hold values of different types. It does so by using a
 * MapVector which contains a vector for each primitive type that is stored.
 * MapVector is used in order to take advantage of its
 * serialization/deserialization methods, as well as the addOrGet method.
 *
 * For performance reasons, UnionVector stores a cached reference to each
 * subtype vector, to avoid having to do the map lookup each time the vector is
 * accessed.
 */
public class UnionVector implements ValueVector {

  public static final int NULL_MARKER = 0;
  public static final String TYPE_VECTOR_NAME = "types";
  public static final String INTERNAL_MAP_NAME = "internal";
  public static final int TYPE_COUNT = MinorType.values().length;

  // Types must be indexed by ordinal, not by number. That is,
  // use type.ordinal(), not type.getNumber().
  public static final MajorType TYPES[] = new MajorType[TYPE_COUNT];

  static {
    for (MinorType minorType : MinorType.values()) {
      TYPES[minorType.ordinal()] = Types.optional(minorType);
    }
  }

  private MaterializedField field;
  private BufferAllocator allocator;
  private Accessor accessor = new Accessor();
  private Mutator mutator = new Mutator();
  private int valueCount;

  /**
   * Map which holds one vector for each subtype, along with a vector that indicates
   * types and the null state. There appears to be no reason other than convenience
   * for using a map. Future implementations may wish to store vectors directly in
   * the union vector, but must then implement the required vector serialization/
   * deserialization and other functionality.
   */

  private MapVector internalMap;

  /**
   * Cached type vector. The vector's permanent location is in the
   * internal map, it is cached for performance. Call
   * {@link #getTypeVector()} to get the cached copy, or to refresh
   * the cache from the internal map if not set.
   */

  private UInt1Vector typeVector;

  /**
   * Set of cached vectors that duplicate vectors store in the
   * internal map. Used to avoid a name lookup on every access.
   * The cache is populated as vectors are added. But, after the
   * union is sent over the wire, the map is populated, but the
   * array is not. It will be repopulated upon first access to
   * the deserialized vectors.
   */

  private ValueVector cachedSubtypes[] = new ValueVector[MinorType.values().length];

  private FieldReader reader;

  private final CallBack callBack;

  public UnionVector(MaterializedField field, BufferAllocator allocator, CallBack callBack) {

    // The metadata may start off listing subtypes for which vectors
    // do not actually exist. It appears that the semantics are to list
    // the subtypes that *could* appear. For example, in a sort we may
    // have two types: one batch has type A, the other type B, but the
    // batches must list both A and B as subtypes.

    this.field = field.clone();
    this.allocator = allocator;
    this.internalMap = new MapVector(INTERNAL_MAP_NAME, allocator, callBack);
    this.typeVector = internalMap.addOrGet(TYPE_VECTOR_NAME, Types.required(MinorType.UINT1), UInt1Vector.class);
    this.field.addChild(internalMap.getField().clone());
    this.callBack = callBack;
  }

  @Override
  public BufferAllocator getAllocator() {
    return allocator;
  }

  public List<MinorType> getSubTypes() {
    return field.getType().getSubTypeList();
  }

  @SuppressWarnings("unchecked")
  public <T extends ValueVector> T subtype(MinorType type) {
    return (T) cachedSubtypes[type.ordinal()];
  }

  /**
   * Add an externally-created subtype vector. The vector must represent a type that
   * does not yet exist in the union, and must be of OPTIONAL mode. Does not call
   * the callback since the client (presumably) knows that it is adding the type.
   * The caller must also allocate the buffer for the vector.
   *
   * @param vector subtype vector to add
   */

  public void addType(ValueVector vector) {
    MinorType type = vector.getField().getType().getMinorType();
    assert subtype(type) == null;
    assert vector.getField().getType().getMode() == DataMode.OPTIONAL;
    assert vector.getField().getName().equals(type.name().toLowerCase());
    cachedSubtypes[type.ordinal()] = vector;
    internalMap.putChild(type.name(), vector);
    addSubType(type);
  }

  public void addSubType(MinorType type) {
    if (field.getType().getSubTypeList().contains(type)) {
      return;
    }
    field.replaceType(
        MajorType.newBuilder(field.getType()).addSubType(type).build());
    if (callBack != null) {
      callBack.doWork();
    }
  }

  /**
   * "Classic" way to add a subtype when working directly with a union vector.
   * Creates the vector, adds it to the internal structures and creates a
   * new buffer of the default size.
   *
   * @param type the type to add
   * @param vectorClass class of the vector to create
   * @return typed form of the new value vector
   */

  private <T extends ValueVector> T classicAddType(MinorType type, Class<? extends ValueVector> vectorClass) {
    int vectorCount = internalMap.size();
    @SuppressWarnings("unchecked")
    T vector = (T) internalMap.addOrGet(type.name().toLowerCase(), TYPES[type.ordinal()], vectorClass);
    cachedSubtypes[type.ordinal()] = vector;
    if (internalMap.size() > vectorCount) {
      vector.allocateNew();
      addSubType(type);
      if (callBack != null) {
        callBack.doWork();
      }
    }
    return vector;
  }

  public MapVector getMap() {
    MapVector mapVector = subtype(MinorType.MAP);
    if (mapVector == null) {
      mapVector = classicAddType(MinorType.MAP, MapVector.class);
    }
    return mapVector;
  }

  public DictVector getDict() {
    DictVector dictVector = subtype(MinorType.DICT);
    if (dictVector == null) {
      dictVector = classicAddType(MinorType.DICT, DictVector.class);
    }
    return dictVector;
  }

  public ListVector getList() {
    ListVector listVector = subtype(MinorType.LIST);
    if (listVector == null) {
      listVector = classicAddType(MinorType.LIST, ListVector.class);
    }
    return listVector;
  }
  <#-- Generating a method per type is probably overkill. However, existing code
       depends on these methods, so didn't want to remove them. Over time, a
       generic, parameterized addOrGet(MinorType type) would be more compact.
       Would need a function to map from minor type to vector class, which
       can be generated here or in TypeHelper. -->
  <#list vv.types as type>
    <#list type.minor as minor>
      <#assign name = minor.class?cap_first />
      <#assign fields = minor.fields!type.fields />
      <#assign uncappedName = name?uncap_first/>
      <#if !minor.class?starts_with("Decimal")>

  public Nullable${name}Vector get${name}Vector() {
    Nullable${name}Vector vector = subtype(MinorType.${name?upper_case});
    if (vector == null) {
      vector = classicAddType(MinorType.${name?upper_case}, Nullable${name}Vector.class);
    }
    return vector;
  }
      </#if>
    </#list>
  </#list>

  /**
   * Add or get a type member given the type.
   *
   * @param type the type of the vector to retrieve
   * @return the (potentially newly created) vector that backs the given type
   */

  public ValueVector getMember(MinorType type) {
    switch (type) {
    case MAP:
      return getMap();
    case LIST:
      return getList();
    case DICT:
      return getDict();
  <#-- This awkard switch statement and call to type-specific method logic
       can be generalized as described above. -->
  <#list vv.types as type>
    <#list type.minor as minor>
      <#assign name = minor.class?cap_first />
      <#assign fields = minor.fields!type.fields />
      <#assign uncappedName = name?uncap_first/>
      <#if !minor.class?starts_with("Decimal")>
    case ${name?upper_case}:
      return get${name}Vector();
      </#if>
    </#list>
  </#list>
    default:
      throw new UnsupportedOperationException(type.toString());
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends ValueVector> T member(MinorType type) {
    return (T) getMember(type);
  }

  public int getTypeValue(int index) {
    return getTypeVector().getAccessor().get(index);
  }

  public UInt1Vector getTypeVector() {
    if (typeVector == null) {
      typeVector = (UInt1Vector) internalMap.getChild(TYPE_VECTOR_NAME);
    }
    return typeVector;
  }

  @VisibleForTesting
  public MapVector getTypeMap() {
    return internalMap;
  }

  @Override
  public void allocateNew() throws OutOfMemoryException {
    internalMap.allocateNew();
    getTypeVector().zeroVector();
  }

  public void allocateNew(int rowCount) throws OutOfMemoryException {
    // The map vector does not have a form that takes a row count,
    // but it should.
    internalMap.allocateNew();
    getTypeVector().zeroVector();
  }

  @Override
  public boolean allocateNewSafe() {
    boolean safe = internalMap.allocateNewSafe();
    if (safe) {
      getTypeVector().zeroVector();
    }
    return safe;
  }

  @Override
  public void setInitialCapacity(int numRecords) { }

  @Override
  public int getValueCapacity() {
    return Math.min(getTypeVector().getValueCapacity(), internalMap.getValueCapacity());
  }

  @Override
  public void close() { }

  @Override
  public void clear() {
    internalMap.clear();
  }

  @Override
  public MaterializedField getField() { return field; }

  @Override
  public void collectLedgers(Set<BufferLedger> ledgers) {
    internalMap.collectLedgers(ledgers);
  }

  @Override
  public int getPayloadByteCount(int valueCount) {
    return internalMap.getPayloadByteCount(valueCount);
  }

  @Override
  public TransferPair getTransferPair(BufferAllocator allocator) {
    return new TransferImpl(field, allocator);
  }

  @Override
  public TransferPair getTransferPair(String ref, BufferAllocator allocator) {
    return new TransferImpl(field.withPath(ref), allocator);
  }

  @Override
  public TransferPair makeTransferPair(ValueVector target) {
    return new TransferImpl((UnionVector) target);
  }

  public void transferTo(UnionVector target) {
    internalMap.makeTransferPair(target.internalMap).transfer();
    target.valueCount = valueCount;
  }

  public void copyFrom(int inIndex, int outIndex, UnionVector from) {
    from.getReader().setPosition(inIndex);
    getWriter().setPosition(outIndex);
    ComplexCopier.copy(from.reader, mutator.writer);
  }

  public void copyFromSafe(int inIndex, int outIndex, UnionVector from) {
    copyFrom(inIndex, outIndex, from);
  }

  @Override
  public void copyEntry(int toIndex, ValueVector from, int fromIndex) {
    copyFromSafe(fromIndex, toIndex, (UnionVector) from);
  }

  /**
   * Add a vector that matches the argument. Transfer the buffer from the argument
   * to the new vector.
   *
   * @param v the vector to clone and add
   * @return the cloned vector that now holds the data from the argument
   */

  public ValueVector addVector(ValueVector v) {
    String name = v.getField().getType().getMinorType().name().toLowerCase();
    MajorType type = v.getField().getType();
    MinorType minorType = type.getMinorType();
    Preconditions.checkState(internalMap.getChild(name) == null, String.format("%s vector already exists", name));
    final ValueVector newVector = internalMap.addOrGet(name, type, BasicTypeHelper.getValueVectorClass(minorType, type.getMode()));
    v.makeTransferPair(newVector).transfer();
    internalMap.putChild(name, newVector);
    cachedSubtypes[minorType.ordinal()] = newVector;
    addSubType(minorType);
    return newVector;
  }

  // Called from SchemaUtil

  public ValueVector setFirstType(ValueVector v, int newValueCount) {

    // We can't check that this really is the first subtype since
    // the subtypes can be declared before vectors are added.

    Preconditions.checkState(accessor.getValueCount() == 0);
    final ValueVector vv = addVector(v);
    MinorType type = v.getField().getType().getMinorType();
    ValueVector.Accessor vAccessor = vv.getAccessor();
    for (int i = 0; i < newValueCount; i++) {
      if (! vAccessor.isNull(i)) {
        mutator.setType(i, type);
      } else {
        mutator.setNull(i);
      }
    }
    mutator.setValueCount(newValueCount);
    return vv;
  }

  @Override
  public void toNullable(ValueVector nullableVector) {
    throw new UnsupportedOperationException();
  }

  private class TransferImpl implements TransferPair {

    private final UnionVector to;

    public TransferImpl(MaterializedField field, BufferAllocator allocator) {
      to = new UnionVector(field, allocator, null);
    }

    public TransferImpl(UnionVector to) {
      this.to = to;
    }

    @Override
    public void transfer() {
      transferTo(to);
    }

    @Override
    public void splitAndTransfer(int startIndex, int length) { }

    @Override
    public ValueVector getTo() {
      return to;
    }

    @Override
    public void copyValueSafe(int from, int to) {
      this.to.copyFrom(from, to, UnionVector.this);
    }
  }

  @Override
  public Accessor getAccessor() { return accessor; }

  @Override
  public Mutator getMutator() { return mutator; }

  @Override
  public FieldReader getReader() {
    if (reader == null) {
      reader = new UnionReader(this);
    }
    return reader;
  }

  public FieldWriter getWriter() {
    if (mutator.writer == null) {
      mutator.writer = new UnionWriter(this);
    }
    return mutator.writer;
  }

  @Override
  public UserBitShared.SerializedField getMetadata() {
    return getField()
            .getAsBuilder()
            .setBufferLength(getBufferSize())
            .setValueCount(valueCount)
            .addChild(internalMap.getMetadata())
            .build();
  }

  @Override
  public int getBufferSize() {
    return internalMap.getBufferSize();
  }

  @Override
  public int getAllocatedSize() {
    return internalMap.getAllocatedSize();
  }

  @Override
  public int getBufferSizeFor(final int valueCount) {
    if (valueCount == 0) {
      return 0;
    }

    long bufferSize = 0;
    for (final ValueVector v : (Iterable<ValueVector>) this) {
      bufferSize += v.getBufferSizeFor(valueCount);
    }

    return (int) bufferSize;
  }

  @Override
  public DrillBuf[] getBuffers(boolean clear) {
    return internalMap.getBuffers(clear);
  }

  @Override
  public void load(UserBitShared.SerializedField metadata, DrillBuf buffer) {
    valueCount = metadata.getValueCount();

    internalMap.load(metadata.getChild(0), buffer);
  }

  @Override
  public Iterator<ValueVector> iterator() {
    return internalMap.iterator();
  }

  public class Accessor extends BaseValueVector.BaseAccessor {

    @Override
    public Object getObject(int index) {
      int ordinal = getTypeVector().getAccessor().get(index);
      if (ordinal == NULL_MARKER) {
        return null;
      }
      // Warning: do not use valueOf as that uses Protobuf
      // field numbers, not enum ordinals.
      MinorType type = MinorType.values()[ordinal];
      switch (type) {
      <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
      <#assign fields = minor.fields!type.fields />
      <#assign uncappedName = name?uncap_first/>
      <#if !minor.class?starts_with("Decimal")>
      case ${name?upper_case}:
        return get${name}Vector().getAccessor().getObject(index);
      </#if>
      </#list></#list>
      case MAP:
        return getMap().getAccessor().getObject(index);
      case LIST:
        return getList().getAccessor().getObject(index);
      case DICT:
        return getDict().getAccessor().getObject(index);
      default:
        throw new UnsupportedOperationException("Cannot support type: " + type.name());
      }
    }

    public byte[] get(int index) { return null; }

    public void get(int index, ComplexHolder holder) { }

    public void get(int index, UnionHolder holder) {
      FieldReader reader = new UnionReader(UnionVector.this);
      reader.setPosition(index);
      holder.reader = reader;
    }

    @Override
    public int getValueCount() { return valueCount; }

    @Override
    public boolean isNull(int index) {

      // Note that type code == 0 is used to indicate a null.
      // This corresponds to the LATE type, not the NULL type.
      // This is presumably an artifact of an earlier implementation...

      return getTypeVector().getAccessor().get(index) == NULL_MARKER;
    }

    public int isSet(int index) {
      return isNull(index) ? 0 : 1;
    }
  }

  public class Mutator extends BaseValueVector.BaseMutator {

    protected UnionWriter writer;

    @Override
    public void setValueCount(int valueCount) {
      UnionVector.this.valueCount = valueCount;

      // Get each claimed child type. This will force creation
      // of the vector in the internal map so that we can properly
      // set the size of that vector.
      //
      // TODO: This is a waste, but the semantics of this class
      // are murky: it is not fully supported. Without this, if we
      // ask for the child type vector later, it will have zero values
      // even if this Union has a non-zero count.
      //
      // A better long-term solution would be to not add types that
      // are not needed, or to remove those types here. In either case,
      // the internal structure will be consistent with the claimed
      // metadata (every type in metadata will have a matching vector in
      // the internal map.)

      for (MinorType type : getSubTypes()) {
        getMember(type);
      }
      internalMap.getMutator().setValueCount(valueCount);
    }

    public void setSafe(int index, UnionHolder holder) {
      FieldReader reader = holder.reader;
      if (writer == null) {
        writer = new UnionWriter(UnionVector.this);
      }
      writer.setPosition(index);
      MinorType type = reader.getType().getMinorType();
      switch (type) {
      <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
      <#assign fields = minor.fields!type.fields />
      <#assign uncappedName = name?uncap_first/>
      <#if !minor.class?starts_with("Decimal")>
      case ${name?upper_case}:
        Nullable${name}Holder ${uncappedName}Holder = new Nullable${name}Holder();
        reader.read(${uncappedName}Holder);
        setSafe(index, ${uncappedName}Holder);
        break;
      </#if>
      </#list></#list>
      case MAP:
        ComplexCopier.copy(reader, writer);
        break;
      case LIST:
        ComplexCopier.copy(reader, writer);
        break;
      default:
        throw new UnsupportedOperationException();
      }
    }

    <#list vv.types as type><#list type.minor as minor><#assign name = minor.class?cap_first />
    <#assign fields = minor.fields!type.fields />
    <#assign uncappedName = name?uncap_first/>
    <#if !minor.class?starts_with("Decimal")>
    public void setSafe(int index, Nullable${name}Holder holder) {
      setType(index, MinorType.${name?upper_case});
      get${name}Vector().getMutator().setSafe(index, holder);
    }

    </#if>
    </#list></#list>

    public void setType(int index, MinorType type) {
      getTypeVector().getMutator().setSafe(index, type.ordinal());
    }

    public void setNull(int index) {
      getTypeVector().getMutator().setSafe(index, NULL_MARKER);
    }

    @Override
    public void reset() { }

    @Override
    public void generateTestData(int values) { }
  }

  @Override
  public void exchange(ValueVector other) {
    throw new UnsupportedOperationException("Union vector does not yet support exchange()");
  }
}
