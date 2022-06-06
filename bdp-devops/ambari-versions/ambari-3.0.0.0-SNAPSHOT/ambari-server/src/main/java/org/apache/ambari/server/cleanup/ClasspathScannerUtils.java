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

package org.apache.ambari.server.cleanup;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.ClassPath;

/**
 * Utility for looking up classes on the classpath that are potentially subject to be bound by a multibinder.
 */
public class ClasspathScannerUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathScannerUtils.class);

  /**
   * Scans the classpath for classes based on the provided arguments
   *
   * @param packageName
   *          the package to be scanned
   * @param exclusions
   *          a list with classes excluded from the result
   * @param selectors
   *          a list with annotation and interface classes that identify classes
   *          to be found (lookup criteria)
   * @return a list of classes from the classpath that match the lookup criteria
   */
  public static Set<Class<?>> findOnClassPath(String packageName, List<Class<?>> exclusions,
      List<Class<?>> selectors) {
    return ClasspathScannerUtils.findOnClassPath(ClasspathScannerUtils.class.getClassLoader(),
        packageName, exclusions, selectors);
  }

  /**
   * Scans the classpath for classes based on the provided arguments
   *
   * @param classLoader
   *          the classloader which should be used for searching.
   * @param packageName
   *          the package to be scanned
   * @param exclusions
   *          a list with classes excluded from the result
   * @param selectors
   *          a list with annotation and interface classes that identify classes
   *          to be found (lookup criteria)
   * @return a list of classes from the classpath that match the lookup criteria
   */
  public static Set<Class<?>> findOnClassPath(ClassLoader classLoader, String packageName,
      List<Class<?>> exclusions, List<Class<?>> selectors) {

    Set<Class<?>> bindingSet = new LinkedHashSet<>();
    try {
      ClassPath classpath = ClassPath.from(classLoader);
      LOGGER.info("Checking package [{}] for binding candidates.", packageName);

      for (ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(packageName)) {
        Class<?> candidate = classInfo.load();

        if (exclusions.contains(candidate)) {
          LOGGER.debug("Candidate [{}] is excluded excluded.", candidate);
          continue;
        }

        if (isEligible(candidate, selectors)) {
          LOGGER.debug("Found class [{}]", candidate);
          bindingSet.add(candidate);
        } else {
          LOGGER.debug("Candidate [{}] doesn't match.", candidate);
        }
      }

    } catch (IOException e) {
      LOGGER.error("Failure during configuring JUICE bindings.", e);
      throw new IllegalArgumentException(e);
    }
    return bindingSet;
  }


  /**
   * Checks whether the candidate class matches lookup conditions.
   *
   * @param candidate the type to be checked
   * @return true if the class matches, false otherwise
   */
  private static boolean isEligible(Class<?> candidate, List<Class<?>> selectors) {
    return checkSubClasses(candidate, selectors) || checkAnnotations(candidate, selectors);
  }

  /**
   * Checks if the candidate has annotations listed in the selection criteria
   *
   * @param candidate the type to be checked
   * @return true if the candidate has annotations listed in the selection criteria, false otherwise
   */
  private static boolean checkAnnotations(Class<?> candidate, List<Class<?>> selectors) {
    LOGGER.debug("Checking annotations for: [{}]", candidate);
    boolean ret = false;
    for (Annotation candidateAnn : candidate.getDeclaredAnnotations()) {
      if (selectors.contains(candidateAnn.annotationType())) {
        ret = true;
        break;
      }
    }
    return ret;
  }

  /**
   * Checks if the candidate implements interfaces listed in the selection criteria
   *
   * @param candidate the type to be checked
   * @return true if the candidate implements interfaces listed in the selection criteria, false otherwise
   */
  private static boolean checkSubClasses(Class<?> candidate, List<Class<?>> selectors) {
    boolean ret = false;
    LOGGER.debug("Checking interfaces for: [{}]", candidate);
    List interfaces = ClassUtils.getAllInterfaces(candidate);

    for (Class selectorItf : selectors) {
      if (interfaces.contains(selectorItf)) {
        LOGGER.debug("Checking candidate for subclassing interface: ", selectorItf);
        if (selectorItf.getClass().isAssignableFrom(candidate.getClass())) {
          ret = true;
          break;
        }
      }
    }
    return ret;
  }
}
