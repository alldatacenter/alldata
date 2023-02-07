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
package org.apache.drill.exec.store.openTSDB;

import org.apache.drill.shaded.guava.com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

// based on https://github.com/apache/spark/blob/master/core/src/main/scala/org/apache/spark/util/SizeEstimator.scala
// which itself is based on https://www.infoworld.com/article/2077408/sizeof-for-java.html
class SizeEstimator {

  private static final Logger logger = LoggerFactory.getLogger(SizeEstimator.class);
  // Estimate the size of arrays larger than ARRAY_SIZE_FOR_SAMPLING by sampling.
  private static final int ARRAY_SIZE_FOR_SAMPLING = 400;
  private static final int ARRAY_SAMPLE_SIZE = 100; // should be lower than ARRAY_SIZE_FOR_SAMPLING

  /**
   * The state of an ongoing size estimation. Contains a stack of objects to visit as well as an
   * IdentityHashMap of visited objects, and provides utility methods for enqueueing new objects
   * to visit.
   */
  private static class SearchState {

    private final IdentityHashMap<Object, Object> visited;
    private final LinkedList<Object> stack = new LinkedList<>();
    long size = 0L;

    SearchState(IdentityHashMap<Object, Object> visited) {
      this.visited = visited;
    }

    void enqueue(Object obj) {
      if (obj != null && !visited.containsKey(obj)) {
        visited.put(obj, null);
        stack.add(obj);
      }
    }

    boolean isFinished() {
      return stack.isEmpty();
    }

    Object dequeue() {
      return stack.removeLast();
    }
  }

  /**
   * Cached information about each class. We remember two things: the "shell size" of the class
   * (size of all non-static fields plus the java.lang.Object size), and any fields that are
   * pointers to objects.
   */
  private static class ClassInfo {
    private final long shellSize;
    private final LinkedList<Field> pointerFields;

    ClassInfo(final long shellSize, final LinkedList<Field> pointerFields) {
      this.shellSize = shellSize;
      this.pointerFields = pointerFields;
    }

    long getShellSize() {
      return shellSize;
    }

    LinkedList<Field> getPointerFields() {
      return pointerFields;
    }
  }

  // Sizes of primitive types
  private static final int BYTE_SIZE    = 1;
  private static final int BOOLEAN_SIZE = 1;
  private static final int CHAR_SIZE    = 2;
  private static final int SHORT_SIZE   = 2;
  private static final int INT_SIZE     = 4;
  private static final int LONG_SIZE    = 8;
  private static final int FLOAT_SIZE   = 4;
  private static final int DOUBLE_SIZE  = 8;

  // Fields can be primitive types, sizes are: 1, 2, 4, 8. Or fields can be pointers. The size of
  // a pointer is 4 or 8 depending on the JVM (32-bit or 64-bit) and UseCompressedOops flag.
  // The sizes should be in descending order, as we will use that information for fields placement.
  private static final int MAX_FIELD_SIZE = 8;
  private static final List<Integer> fieldSizes = new ArrayList<>();

  // Alignment boundary for objects
  // TODO: Is this arch dependent ?
  private static final int ALIGN_SIZE = 8;

  // A cache of ClassInfo objects for each class
  private static final Map<Class<?>, ClassInfo> classInfos = new MapMaker().weakKeys().makeMap();

  // Object and pointer sizes are arch dependent
  private static final boolean is64bit;

  private static int pointerSize = 4;

  // Minimum size of a java.lang.Object
  private static int objectSize = 8;

  static {
    fieldSizes.add(8);
    fieldSizes.add(4);
    fieldSizes.add(2);
    fieldSizes.add(1);

    // Sets object size, pointer size based on architecture and CompressedOops settings
    // from the JVM.
    final String arch = System.getProperty("os.arch");
    is64bit = arch.contains("64") || arch.contains("s390x");
    // Size of an object reference
    // Based on https://wikis.oracle.com/display/HotSpotInternals/CompressedOops
    final boolean isCompressedOops = getIsCompressedOops();

    objectSize = !is64bit ? 8 : (!isCompressedOops ? 16 : 12);
    pointerSize = (is64bit && !isCompressedOops) ? 8 : 4;
    classInfos.clear();
    classInfos.put(Object.class, new ClassInfo(objectSize, new LinkedList<>()));
  }

  private static boolean getIsCompressedOops() {
    // This is only used by tests to override the detection of compressed oops. The test
    // actually uses a system property instead of a SparkConf, so we'll stick with that.
    if (System.getProperty("spark.test.useCompressedOops") != null) {
      return Boolean.getBoolean("spark.test.useCompressedOops");
    }

    final String javaVendor = System.getProperty("java.vendor");
    if (javaVendor.contains("IBM") || javaVendor.contains("OpenJ9")) {
      return System.getProperty("java.vm.info").contains("Compressed Ref");
    }

    try {
      final String hotSpotMBeanName = "com.sun.management:type=HotSpotDiagnostic";
      final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

      // NOTE: This should throw an exception in non-Sun JVMs
      final Class<?> hotSpotMBeanClass = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
      final Method getVMMethod = hotSpotMBeanClass.getDeclaredMethod("getVMOption",
        Class.forName("java.lang.String"));

      final Object bean = ManagementFactory.newPlatformMXBeanProxy(server,
        hotSpotMBeanName, hotSpotMBeanClass);
      // TODO: We could use reflection on the VMOption returned ?
      return getVMMethod.invoke(bean, "UseCompressedOops").toString().contains("true");
    } catch(Exception e) {
      // Guess whether they've enabled UseCompressedOops based on whether maxMemory < 32 GB
      final boolean guess = Runtime.getRuntime().maxMemory() < (32L*1024*1024*1024);
      logger.warn("Failed to check whether UseCompressedOops is set; assuming {}", guess);
      return guess;
    }
  }

  /**
   * Estimate the number of bytes that the given object takes up on the JVM heap. The estimate
   * includes space taken up by objects referenced by the given object, their references, and so on
   * and so forth.
   *
   * This is useful for determining the amount of heap space a broadcast variable will occupy on
   * each executor or the amount of space each object will take when caching objects in
   * deserialized form. This is not the same as the serialized size of the object, which will
   * typically be much smaller.
   */
  public static long estimate(final Object obj) {
    return estimate(obj, new IdentityHashMap<>());
  }

  private static long estimate(final Object obj, final IdentityHashMap<Object, Object> visited) {
    final SearchState state = new SearchState(visited);
    state.enqueue(obj);
    while (!state.isFinished()) {
      visitSingleObject(state.dequeue(), state);
    }
    return state.size;
  }

  private static void visitSingleObject(final Object obj, final SearchState state) {
    final Class<?> cls = obj.getClass();
    if (cls.isArray()) {
      visitArray(obj, cls, state);
    } else if (cls.getName().startsWith("scala.reflect")) {
      // Many objects in the scala.reflect package reference global reflection objects which, in
      // turn, reference many other large global objects. Do nothing in this case.
    } else if (obj instanceof ClassLoader || obj instanceof Class<?>) {
      // Hadoop JobConfs created in the interpreter have a ClassLoader, which greatly confuses
      // the size estimator since it references the whole REPL. Do nothing in this case. In
      // general all ClassLoaders and Classes will be shared between objects anyway.
    } else {
      final Long calculatedSize = knownSize(obj);
      if (calculatedSize != null) {
        state.size += calculatedSize;
      } else {
        final ClassInfo classInfo = getClassInfo(cls);
        state.size += alignSize(classInfo.shellSize);
        for (Field field : classInfo.pointerFields) {
          try {
            state.enqueue(field.get(obj));
          } catch (Exception e) {
            //skip this field
          }
        }
      }
    }
  }

  private static void visitArray(final Object array, final Class<?> arrayClass, final SearchState state) {
    final long length = arrayLength(array);
    final Class<?> elementClass = arrayClass.getComponentType();

    // Arrays have object header and length field which is an integer
    long arrSize = alignSize(objectSize + INT_SIZE);

    if (elementClass.isPrimitive()) {
      arrSize += alignSize(length * primitiveSize(elementClass));
      state.size += arrSize;
    } else {
      arrSize += alignSize(length * pointerSize);
      state.size += arrSize;

      if (length <= ARRAY_SIZE_FOR_SAMPLING) {
        int arrayIndex = 0;
        while (arrayIndex < length) {
          state.enqueue(arrayApply(array, arrayIndex));
          arrayIndex += 1;
        }
      } else {
        // Estimate the size of a large array by sampling elements without replacement.
        // To exclude the shared objects that the array elements may link, sample twice
        // and use the min one to calculate array size.
        final Random rand = new Random(42);
        final HashSet<Integer> drawn = new HashSet<>(2 * ARRAY_SAMPLE_SIZE);
        final long s1 = sampleArray(array, state, rand, drawn, (int) length);
        final long s2 = sampleArray(array, state, rand, drawn, (int) length);
        final long size = Math.min(s1, s2);
        state.size += Math.max(s1, s2) +
          (size * ((length - ARRAY_SAMPLE_SIZE) / ARRAY_SAMPLE_SIZE));
      }
    }
  }

  private static long sampleArray(final Object array, final SearchState state, final Random rand,
                                  final HashSet<Integer> drawn, final int length) {
    long size = 0L;
    for (int i = 0; i < ARRAY_SAMPLE_SIZE; i++) {
      int index;
      do {
        index = rand.nextInt(length);
      } while (drawn.contains(index));
      drawn.add(index);
      final Object obj = arrayApply(array, index);
      if (obj != null) {
        size += SizeEstimator.estimate(obj, state.visited);
      }
    }
    return size;
  }

  private static Object arrayApply(final Object obj, final int index) {
    if (obj instanceof Object[]) {
      return ((Object[])obj)[index];
    } else if (obj instanceof byte[]) {
      return ((byte[]) obj)[index];
    } else if (obj instanceof int[]) {
      return ((int[]) obj)[index];
    } else if (obj instanceof short[]) {
      return ((short[])obj)[index];
    } else if (obj instanceof long[]) {
      return ((long[])obj)[index];
    } else if (obj instanceof boolean[]) {
      return ((boolean[]) obj)[index];
    } else if (obj instanceof float[]) {
      return ((float[]) obj)[index];
    } else if (obj instanceof double[]) {
      return ((double[]) obj)[index];
    } else if (obj instanceof char[]) {
      return ((char[]) obj)[index];
    }
    throw new IllegalArgumentException("illegal input for arrayApply " + obj);
  }

  private static int arrayLength(final Object obj) {
    if (obj instanceof Object[]) {
      return ((Object[])obj).length;
    } else if (obj instanceof byte[]) {
      return ((byte[]) obj).length;
    } else if (obj instanceof int[]) {
      return ((int[]) obj).length;
    } else if (obj instanceof short[]) {
      return ((short[])obj).length;
    } else if (obj instanceof long[]) {
      return ((long[])obj).length;
    } else if (obj instanceof boolean[]) {
      return ((boolean[]) obj).length;
    } else if (obj instanceof float[]) {
      return ((float[]) obj).length;
    } else if (obj instanceof double[]) {
      return ((double[])obj).length;
    } else if (obj instanceof char[]) {
      return ((char[]) obj).length;
    }
    throw new IllegalArgumentException("illegal input for arrayLength " + obj);
  }

  private static int primitiveSize(Class<?> cls) {
    if (cls == byte.class) {
      return BYTE_SIZE;
    } else if (cls == boolean.class) {
      return BOOLEAN_SIZE;
    } else if (cls == char.class) {
      return CHAR_SIZE;
    } else if (cls == short.class) {
      return SHORT_SIZE;
    } else if (cls == int.class) {
      return INT_SIZE;
    } else if (cls == long.class) {
      return LONG_SIZE;
    } else if (cls == float.class) {
      return FLOAT_SIZE;
    } else if (cls == double.class) {
      return DOUBLE_SIZE;
    } else {
      throw new IllegalArgumentException(
        "Non-primitive class " + cls + " passed to primitiveSize()");
    }
  }

  /**
   * Get or compute the ClassInfo for a given class.
   */
  private static ClassInfo getClassInfo(Class<?> cls) {
    // Check whether we've already cached a ClassInfo for this class
    final ClassInfo info = classInfos.get(cls);
    if (info != null) {
      return info;
    }

    final ClassInfo parent = getClassInfo(cls.getSuperclass());
    long shellSize = parent.getShellSize();
    LinkedList<Field> pointerFields = parent.getPointerFields();
    final int[] sizeCount = new int[MAX_FIELD_SIZE + 1];

    for (Field field : cls.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        final Class<?> fieldClass = field.getType();
        if (fieldClass.isPrimitive()) {
          sizeCount[primitiveSize(fieldClass)] += 1;
        } else {
          // Note: in Java 9+ this would be better with trySetAccessible and canAccess
          try {
            field.setAccessible(true); // Enable future get()'s on this field
            pointerFields.add(0, field);
          } catch(SecurityException se) {
            // do nothing
            // Java 9+ can throw InaccessibleObjectException but the class is Java 9+-only
          } catch (RuntimeException re) {
            if (re.getClass().getSimpleName().equals("InaccessibleObjectException")) {
              // do nothing
            } else {
              throw re;
            }
          }
          sizeCount[pointerSize] += 1;
        }
      }
    }

    // Based on the simulated field layout code in Aleksey Shipilev's report:
    // http://cr.openjdk.java.net/~shade/papers/2013-shipilev-fieldlayout-latest.pdf
    // The code is in Figure 9.
    // The simplified idea of field layout consists of 4 parts (see more details in the report):
    //
    // 1. field alignment: HotSpot lays out the fields aligned by their size.
    // 2. object alignment: HotSpot rounds instance size up to 8 bytes
    // 3. consistent fields layouts throughout the hierarchy: This means we should layout
    // superclass first. And we can use superclass's shellSize as a starting point to layout the
    // other fields in this class.
    // 4. class alignment: HotSpot rounds field blocks up to HeapOopSize not 4 bytes, confirmed
    // with Aleksey. see https://bugs.openjdk.java.net/browse/CODETOOLS-7901322
    //
    // The real world field layout is much more complicated. There are three kinds of fields
    // order in Java 8. And we don't consider the @contended annotation introduced by Java 8.
    // see the HotSpot classloader code, layout_fields method for more details.
    // hg.openjdk.java.net/jdk8/jdk8/hotspot/file/tip/src/share/vm/classfile/classFileParser.cpp
    long alignedSize = shellSize;
    for (Integer size : fieldSizes) {
      if (sizeCount[size] > 0) {
        final long count = sizeCount[size];
        // If there are internal gaps, smaller field can fit in.
        alignedSize = Math.max(alignedSize, alignSizeUp(shellSize, size) + size * count);
        shellSize += size * count;
      }
    }

    // Should choose a larger size to be new shellSize and clearly alignedSize >= shellSize, and
    // round up the instance filed blocks
    shellSize = alignSizeUp(alignedSize, pointerSize);

    // Create and cache a new ClassInfo
    final ClassInfo newInfo = new ClassInfo(shellSize, pointerFields);
    classInfos.put(cls, newInfo);
    return newInfo;
  }

  private static long alignSize(final long size) {
    return alignSizeUp(size, ALIGN_SIZE);
  }

  /**
   * Compute aligned size. The alignSize must be 2^n, otherwise the result will be wrong.
   * When alignSize = 2^n, alignSize - 1 = 2^n - 1. The binary representation of (alignSize - 1)
   * will only have n trailing 1s(0b00...001..1). ~(alignSize - 1) will be 0b11..110..0. Hence,
   * (size + alignSize - 1) & ~(alignSize - 1) will set the last n bits to zeros, which leads to
   * multiple of alignSize.
   */
  private static long alignSizeUp(final long size, final int alignSize) {
    return (size + alignSize - 1) & ~(alignSize - 1);
  }

  private static Long knownSize(final Object obj) {
    try {
      final Method method = obj.getClass().getMethod("estimatedSize");
      return (Long) method.invoke(obj);
    } catch (Exception e) {
      return null;
    }
  }
}
