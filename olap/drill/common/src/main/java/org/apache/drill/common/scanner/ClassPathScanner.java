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
package org.apache.drill.common.scanner;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.scanner.persistence.AnnotationDescriptor;
import org.apache.drill.common.scanner.persistence.AttributeDescriptor;
import org.apache.drill.common.scanner.persistence.ChildClassDescriptor;
import org.apache.drill.common.scanner.persistence.FieldDescriptor;
import org.apache.drill.common.scanner.persistence.AnnotatedClassDescriptor;
import org.apache.drill.common.scanner.persistence.ParentClassDescriptor;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.reflections.Reflections;
import org.reflections.adapters.JavassistAdapter;
import org.reflections.scanners.AbstractScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.HashMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.MemberValueVisitor;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * Classpath scanning utility.
 * The classpath should be scanned once at startup from a DrillConfig instance. {@link ClassPathScanner#fromPrescan(DrillConfig)}
 * The DrillConfig provides:
 *  - the list of packages to scan. (drill.classpath.scanning.packages) {@link ClassPathScanner#IMPLEMENTATIONS_SCAN_PACKAGES}
 *  - the list of base classes to scan for implementations. (drill.classpath.scanning.base.classes) {@link ClassPathScanner#IMPLEMENTATIONS_SCAN_CLASSES}
 *  - the list of annotations to scan for. (drill.classpath.scanning.annotations) {@link ClassPathScanner#IMPLEMENTATIONS_SCAN_ANNOTATIONS}
 * Only the class directories and jars containing a drill-module.conf will be scanned.
 * Drill core packages are scanned at build time and the result is saved in a JSON file.
 * At runtime only the locations that have not been scanned yet will be scanned.
 */
public final class ClassPathScanner {
  private static final Logger logger = LoggerFactory.getLogger(ClassPathScanner.class);
  private static final JavassistAdapter METADATA_ADAPTER = new JavassistAdapter();

  /** Configuration pathname to list of names of packages to scan for implementations. */
  private static final String IMPLEMENTATIONS_SCAN_PACKAGES = "drill.classpath.scanning.packages";

  /** Configuration pathname to list of names of base classes to scan for implementations. */
  private static final String IMPLEMENTATIONS_SCAN_CLASSES = "drill.classpath.scanning.base.classes";

  /** Configuration pathname to list of names of annotations to scan for. */
  private static final String IMPLEMENTATIONS_SCAN_ANNOTATIONS = "drill.classpath.scanning.annotations";

  /** Configuration pathname to turn off build time caching. */
  public static final String IMPLEMENTATIONS_SCAN_CACHE = "drill.classpath.scanning.cache.enabled";

  /**
   * scans the inheritance tree
   */
  private static class SubTypesScanner extends AbstractScanner {

    private final Multimap<String, ChildClassDescriptor> parentsChildren = HashMultimap.create();
    private final Multimap<String, ChildClassDescriptor> children = HashMultimap.create();

    public SubTypesScanner(List<ParentClassDescriptor> parentImplementations) {
      for (ParentClassDescriptor parentClassDescriptor : parentImplementations) {
        parentsChildren.putAll(parentClassDescriptor.getName(), parentClassDescriptor.getChildren());
      }
    }

    @Override
    public void scan(final Object cls) {
      final ClassFile classFile = (ClassFile)cls;
      String className = classFile.getName();
      String superclass = classFile.getSuperclass();
      boolean isAbstract = (classFile.getAccessFlags() & (AccessFlag.INTERFACE | AccessFlag.ABSTRACT)) != 0;
      ChildClassDescriptor scannedClass = new ChildClassDescriptor(className, isAbstract);
      if (!superclass.equals(Object.class.getName())) {
        children.put(superclass, scannedClass);
      }
      for (String anInterface : classFile.getInterfaces()) {
        children.put(anInterface, scannedClass);
      }
    }

    /**
     * @param name the class name to get all the children of
     * @return the class names of all children direct or indirect
     */
    public Set<ChildClassDescriptor> getChildrenOf(String name) {
      Collection<ChildClassDescriptor> scannedChildren = children.get(name);
      // add all scanned children
      Set<ChildClassDescriptor> result = new HashSet<>(scannedChildren);
      // recursively add children's children
      Collection<ChildClassDescriptor> allChildren = new ArrayList<>();
      allChildren.addAll(scannedChildren);
      allChildren.addAll(parentsChildren.get(name));
      for (ChildClassDescriptor child : allChildren) {
        result.addAll(getChildrenOf(child.getName()));
      }
      return result;
    }

  }

  /**
   * Converts the annotation attribute value into a list of string to simplify
   */
  private static class ListingMemberValueVisitor implements MemberValueVisitor {
    private final List<String> values;

    private ListingMemberValueVisitor(List<String> values) {
      this.values = values;
    }

    @Override
    public void visitStringMemberValue(StringMemberValue node) {
      values.add(node.getValue());
    }

    @Override
    public void visitShortMemberValue(ShortMemberValue node) {
      values.add(String.valueOf(node.getValue()));
    }

    @Override
    public void visitLongMemberValue(LongMemberValue node) {
      values.add(String.valueOf(node.getValue()));
    }

    @Override
    public void visitIntegerMemberValue(IntegerMemberValue node) {
      values.add(String.valueOf(node.getValue()));
    }

    @Override
    public void visitFloatMemberValue(FloatMemberValue node) {
      values.add(String.valueOf(node.getValue()));
    }

    @Override
    public void visitEnumMemberValue(EnumMemberValue node) {
      values.add(node.getValue());
    }

    @Override
    public void visitDoubleMemberValue(DoubleMemberValue node) {
      values.add(String.valueOf(node.getValue()));
    }

    @Override
    public void visitClassMemberValue(ClassMemberValue node) {
      values.add(node.getValue());
    }

    @Override
    public void visitCharMemberValue(CharMemberValue node) {
      values.add(String.valueOf(node.getValue()));
    }

    @Override
    public void visitByteMemberValue(ByteMemberValue node) {
      values.add(String.valueOf(node.getValue()));
    }

    @Override
    public void visitBooleanMemberValue(BooleanMemberValue node) {
      values.add(String.valueOf(node.getValue()));
    }

    @Override
    public void visitArrayMemberValue(ArrayMemberValue node) {
      MemberValue[] nestedValues = node.getValue();
      for (MemberValue v : nestedValues) {
        v.accept(new ListingMemberValueVisitor(values) {
          @Override
          public void visitArrayMemberValue(ArrayMemberValue node) {
            values.add(Arrays.toString(node.getValue()));
          }
        });
      }
    }

    @Override
    public void visitAnnotationMemberValue(AnnotationMemberValue node) {
      values.add(String.valueOf(node.getValue()));
    }
  }

  /**
   * scans functions annotated with configured annotations
   *  and keeps track of its annotations and fields
   */
  private static final class AnnotationScanner extends AbstractScanner {

    private final List<AnnotatedClassDescriptor> functions = new ArrayList<>();

    private final Set<String> annotationsToScan;

    AnnotationScanner(Collection<String> annotationsToScan) {
      super();
      this.annotationsToScan = Collections.unmodifiableSet(new HashSet<>(annotationsToScan));
    }

    public List<AnnotatedClassDescriptor> getAnnotatedClasses() {
      return unmodifiableList(functions);
    }

    @Override
    public void scan(final Object cls) {
      final ClassFile classFile = (ClassFile)cls;
      AnnotationsAttribute annotations = ((AnnotationsAttribute)classFile.getAttribute(AnnotationsAttribute.visibleTag));
      if (annotations != null) {
        boolean isAnnotated = false;
        for (javassist.bytecode.annotation.Annotation a : annotations.getAnnotations()) {
          if (annotationsToScan.contains(a.getTypeName())) {
            isAnnotated = true;
          }
        }
        if (isAnnotated) {
          List<AnnotationDescriptor> classAnnotations = getAnnotationDescriptors(annotations);
          List<FieldInfo> classFields = classFile.getFields();
          List<FieldDescriptor> fieldDescriptors = new ArrayList<>(classFields.size());
          for (FieldInfo field : classFields) {
            String fieldName = field.getName();
            AnnotationsAttribute fieldAnnotations = ((AnnotationsAttribute) field.getAttribute(AnnotationsAttribute.visibleTag));
            fieldDescriptors.add(new FieldDescriptor(fieldName, field.getDescriptor(), getAnnotationDescriptors(fieldAnnotations)));
          }
          functions.add(new AnnotatedClassDescriptor(classFile.getName(), classAnnotations, fieldDescriptors));
        }
      }
    }

    private List<AnnotationDescriptor> getAnnotationDescriptors(AnnotationsAttribute annotationsAttr) {
      if (annotationsAttr == null) {
        return Collections.emptyList();
      }
      List<AnnotationDescriptor> annotationDescriptors = new ArrayList<>(annotationsAttr.numAnnotations());
      for (javassist.bytecode.annotation.Annotation annotation : annotationsAttr.getAnnotations()) {
        // Sigh: javassist uses raw collections (is this 2002?)
        Set<String> memberNames = annotation.getMemberNames();
        List<AttributeDescriptor> attributes = new ArrayList<>();
        if (memberNames != null) {
          for (String name : memberNames) {
            MemberValue memberValue = annotation.getMemberValue(name);
            final List<String> values = new ArrayList<>();
            memberValue.accept(new ListingMemberValueVisitor(values));
            attributes.add(new AttributeDescriptor(name, values));
          }
        }
        annotationDescriptors.add(new AnnotationDescriptor(annotation.getTypeName(), attributes));
      }
      return annotationDescriptors;
    }
  }

  /**
   * @return paths that have a drill config file in them
   */
  static Set<URL> getMarkedPaths(String resourcePathName) {
    return forResource(resourcePathName, true);
  }

  public static Collection<URL> getConfigURLs(String resourcePathName) {
    return forResource(resourcePathName, false);
  }

  /**
   * Gets URLs of any classpath resources with given resource pathname.
   *
   * @param  resourcePathname  resource pathname of classpath resource instances
   *           to scan for (relative to specified class loaders' classpath roots)
   * @param  returnRootPathname  whether to collect classpath root portion of
   *           URL for each resource instead of full URL of each resource
   * @return  empty set if none
   */
  public static Set<URL> forResource(final String resourcePathname, final boolean returnRootPathname) {
    logger.debug("Scanning classpath for resources with pathname \"{}\".",
                 resourcePathname);
    final Set<URL> resultUrlSet = new HashSet<>();
    final ClassLoader classLoader = ClassPathScanner.class.getClassLoader();
    try {
      final Enumeration<URL> resourceUrls = classLoader.getResources(resourcePathname);
      while (resourceUrls.hasMoreElements()) {
        final URL resourceUrl = resourceUrls.nextElement();
        logger.trace("- found a(n) {} at {}.", resourcePathname, resourceUrl);
        int index = resourceUrl.toExternalForm().lastIndexOf(resourcePathname);
        if (index != -1 && returnRootPathname) {
          final URL classpathRootUrl = new URL(resourceUrl.toExternalForm().substring(0, index));
          resultUrlSet.add(classpathRootUrl);
          logger.debug("- collected resource's classpath root URL {}.", classpathRootUrl);
        } else {
          resultUrlSet.add(resourceUrl);
          logger.debug("- collected resource URL {}.", resourceUrl);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error scanning for resources named " + resourcePathname, e);
    }
    return resultUrlSet;
  }

  static List<String> getPackagePrefixes(DrillConfig config) {
    return config.getStringList(IMPLEMENTATIONS_SCAN_PACKAGES);
  }

  static List<String> getScannedBaseClasses(DrillConfig config) {
    return config.getStringList(IMPLEMENTATIONS_SCAN_CLASSES);
  }

  static List<String> getScannedAnnotations(DrillConfig config) {
    if (config.hasPath(IMPLEMENTATIONS_SCAN_ANNOTATIONS)) {
      return config.getStringList(IMPLEMENTATIONS_SCAN_ANNOTATIONS);
    } else {
      return Collections.emptyList();
    }
  }

  static boolean isScanBuildTimeCacheEnabled(DrillConfig config) {
    if (config.hasPath(IMPLEMENTATIONS_SCAN_CACHE)) {
      return config.getBoolean(IMPLEMENTATIONS_SCAN_CACHE);
    } else {
      return true; // on by default
    }
  }

  /**
   *
   * @param pathsToScan the locations to scan for .class files
   * @param packagePrefixes the whitelist of package prefixes to scan
   * @param parentResult if there was a prescan, its result
   * @return the merged scan
   */
  static ScanResult scan(Collection<URL> pathsToScan, Collection<String> packagePrefixes, Collection<String> scannedClasses, Collection<String> scannedAnnotations, ScanResult parentResult) {
    Stopwatch watch = Stopwatch.createStarted();
    try {
      AnnotationScanner annotationScanner = new AnnotationScanner(scannedAnnotations);
      SubTypesScanner subTypesScanner = new SubTypesScanner(parentResult.getImplementations());
      if (packagePrefixes.size() > 0) {
        final FilterBuilder filter = new FilterBuilder();
        for (String prefix : packagePrefixes) {
          filter.include(FilterBuilder.prefix(prefix));
        }
        ConfigurationBuilder conf = new ConfigurationBuilder()
            .setUrls(pathsToScan)
            .setMetadataAdapter(METADATA_ADAPTER) // Scanners depend on this
            .filterInputsBy(filter)
            .setScanners(annotationScanner, subTypesScanner);

        // scans stuff, but don't use the funky storage layer
        new Reflections(conf);
      }
      List<ParentClassDescriptor> implementations = new ArrayList<>();
      for (String baseTypeName: scannedClasses) {
        implementations.add(
            new ParentClassDescriptor(
                baseTypeName,
                new ArrayList<>(subTypesScanner.getChildrenOf(baseTypeName))));
      }
      List<AnnotatedClassDescriptor> annotated = annotationScanner.getAnnotatedClasses();
      verifyClassUnicity(annotated, pathsToScan);
      return new ScanResult(
          packagePrefixes,
          scannedClasses,
          scannedAnnotations,
          annotated,
          implementations);
    } finally {
      logger.info(
          format("Scanning packages %s in locations %s took %dms",
              packagePrefixes, pathsToScan, watch.elapsed(MILLISECONDS)));
    }
  }

  private static void verifyClassUnicity(List<AnnotatedClassDescriptor> annotatedClasses, Collection<URL> pathsScanned) {
    Set<String> scanned = new HashSet<>();
    for (AnnotatedClassDescriptor annotated : annotatedClasses) {
      if (!scanned.add(annotated.getClassName())) {
        throw UserException.functionError()
            .message(
                "function %s scanned twice in the following locations:\n"
                + "%s\n"
                + "Do you have conflicting jars on the classpath?",
                annotated.getClassName(), pathsScanned
            )
            .build(logger);
      }
    }
  }

  static ScanResult emptyResult() {
    return new ScanResult(
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  public static ScanResult fromPrescan(DrillConfig config) {
    return RunTimeScan.fromPrescan(config);
  }
}
