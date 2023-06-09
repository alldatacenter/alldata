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
package org.apache.drill.common.scanner.persistence;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.drill.common.exceptions.DrillRuntimeException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.HashMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The root doc of the scan result
 */
public final class ScanResult {
  private static final Logger logger = LoggerFactory.getLogger(ScanResult.class);

  private final List<String> scannedPackages;
  private final Set<String> scannedClasses;
  private final Set<String> scannedAnnotations;
  private final List<AnnotatedClassDescriptor> annotatedClasses;
  private final List<ParentClassDescriptor> implementations;

  private final Map<String, ParentClassDescriptor> parentClassByName;
  private final Multimap<String, AnnotatedClassDescriptor> annotationsByName;

  @JsonCreator public ScanResult(
      @JsonProperty("scannedPackages") Collection<String> scannedPackages,
      @JsonProperty("scannedClasses") Collection<String> scannedClasses,
      @JsonProperty("scannedAnnotations") Collection<String> scannedAnnotations,
      @JsonProperty("annotatedClasses") Collection<AnnotatedClassDescriptor> annotatedClasses,
      @JsonProperty("implementations") Collection<ParentClassDescriptor> implementations) {
    this.scannedPackages = unmodifiableList(new ArrayList<>(checkNotNull(scannedPackages)));
    this.scannedClasses = unmodifiableSet(new HashSet<>(checkNotNull(scannedClasses)));
    this.scannedAnnotations = unmodifiableSet(new HashSet<>(checkNotNull(scannedAnnotations)));
    this.annotatedClasses = unmodifiableList(new ArrayList<>(checkNotNull(annotatedClasses)));
    this.implementations = unmodifiableList(new ArrayList<>(checkNotNull(implementations)));
    this.parentClassByName = new HashMap<>();
    for (ParentClassDescriptor parentClassDescriptor : implementations) {
      this.parentClassByName.put(parentClassDescriptor.getName(), parentClassDescriptor);
    }
    this.annotationsByName = HashMultimap.create();
    for (AnnotatedClassDescriptor annotated : annotatedClasses) {
      for (AnnotationDescriptor annotationDescriptor : annotated.getAnnotations()) {
        if (scannedAnnotations.contains(annotationDescriptor.getAnnotationType())) {
          this.annotationsByName.put(annotationDescriptor.getAnnotationType(), annotated);
        }
      }
    }
  }

  /**
   * @return packages that were scanned as defined in the drill configs
   */
  public List<String> getScannedPackages() {
    return scannedPackages;
  }

  /**
   * @return functions annotated with configured annotations
   */
  public List<AnnotatedClassDescriptor> getAnnotatedClasses() {
    return annotatedClasses;
  }

  /**
   * @return list of implementations per parent class
   */
  public List<ParentClassDescriptor> getImplementations() {
    return implementations;
  }

  public Set<String> getScannedClasses() {
    return scannedClasses;
  }

  public Set<String> getScannedAnnotations() {
    return scannedAnnotations;
  }

  /**
   * @param c the parent class name
   * @return the descriptor for the implementations found
   */
  public ParentClassDescriptor getImplementations(String c) {
    if (!scannedClasses.contains(c)) {
      throw new IllegalArgumentException(
          c + " is not scanned. "
              + "Only implementations for the following classes are scanned: "
              + scannedClasses);
    }
    return parentClassByName.get(c);
  }

  /**
   * Loads all the scanned classes for this parent as a side effect
   * @param c the parent
   * @return all the classes found
   */
  public <T> Set<Class<? extends T>> getImplementations(Class<T> c) {
    ParentClassDescriptor p = getImplementations(c.getName());
    Stopwatch watch = Stopwatch.createStarted();
    Set<Class<? extends T>> result = new HashSet<>();
    try {
      if (p != null) {
        for (ChildClassDescriptor child : p.getChildren()) {
          if (!child.isAbstract()) {
            try {
              result.add(Class.forName(child.getName()).asSubclass(c));
            } catch (ClassNotFoundException e) {
              throw new DrillRuntimeException("scanned class could not be found: " + child.getName(), e);
            }
          }
        }
      }
      return result;
    } finally {
      logger.info(
          format("loading %d classes for %s took %dms",
              result.size(), c.getName(), watch.elapsed(MILLISECONDS)));
    }
  }

  /**
   * @param c the annotation class name
   * @return the descriptor of the annotated class
   */
  public List<AnnotatedClassDescriptor> getAnnotatedClasses(String c) {
    if (!scannedAnnotations.contains(c)) {
      throw new IllegalArgumentException(
          c + " is not scanned. "
              + "Only the following Annotations are scanned: "
              + scannedAnnotations);
    }
    return new ArrayList<>(annotationsByName.get(c));
  }

  @Override
  public String toString() {
    return "ScanResult ["
        + "scannedPackages=" + scannedPackages + ", "
        + "scannedClasses=" + scannedClasses + ", "
        + "scannedAnnotations=" + scannedAnnotations + ", "
        + "annotatedClasses=" + annotatedClasses + ", "
        + "implementations=" + implementations + "]";
  }

  private <T> List<T> merge(Collection<T> a, Collection<T> b) {
    final List<T> newList = new ArrayList<>(a);
    newList.addAll(b);
    return newList;
  }

  /**
   * merges this and other together into a new result object
   * @param other
   * @return the resulting merge
   */
  public ScanResult merge(ScanResult other) {
    final Multimap<String, ChildClassDescriptor> newImpls = HashMultimap.create();
    for (Collection<ParentClassDescriptor> impls : asList(implementations, other.implementations)) {
      for (ParentClassDescriptor c : impls) {
        newImpls.putAll(c.getName(), c.getChildren());
      }
    }
    List<ParentClassDescriptor> newImplementations = new ArrayList<>();
    for (Entry<String, Collection<ChildClassDescriptor>> entry : newImpls.asMap().entrySet()) {
      newImplementations.add(new ParentClassDescriptor(entry.getKey(), new ArrayList<>(entry.getValue())));
    }

    return new ScanResult(
        merge(scannedPackages, other.scannedPackages),
        merge(scannedClasses, other.scannedClasses),
        merge(scannedAnnotations, other.scannedAnnotations),
        merge(annotatedClasses, other.annotatedClasses),
        newImplementations);
  }
}
