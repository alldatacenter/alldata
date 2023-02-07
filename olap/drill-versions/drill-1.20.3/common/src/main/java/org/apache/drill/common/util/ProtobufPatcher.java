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

package org.apache.drill.common.util;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.scopedpool.ScopedClassPoolRepository;
import javassist.scopedpool.ScopedClassPoolRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class ProtobufPatcher {

  private static final Logger logger = LoggerFactory.getLogger(ProtobufPatcher.class);
  private static final String protobufPackage = "com.google.protobuf.";

  private static boolean patchingAttempted = false;

  /**
   * Makes protobuf version 3.6+ compatible to libraries that still use protobuf 2.5.0.
   */
  public static synchronized void patch() {
    if (!patchingAttempted) {
      patchingAttempted = true;
      patchByteString();
      patchGeneratedMessageLite();
      patchGeneratedMessageLiteBuilder();
    }
  }

  /**
   * HBase client overrides methods from {@link com.google.protobuf.ByteString},
   * that were made final in version 3.6+ of protobuf.
   * This method removes the final modifiers. It also creates and loads classes
   * that were made private nested in protobuf 3.6+ to be accessible by the old fully qualified name.
   */
  private static void patchByteString() {
    try {
      ClassPool classPool = getClassPool();
      CtClass byteString = classPool.get(protobufPackage + "ByteString");
      removeFinal(byteString.getDeclaredMethod("toString"));
      removeFinal(byteString.getDeclaredMethod("hashCode"));
      removeFinal(byteString.getDeclaredMethod("iterator"));

      // Need to inherit from these classes to make them accessible by the old path.
      CtClass googleLiteralByteString = classPool.get(protobufPackage + "ByteString$LiteralByteString");
      removePrivate(googleLiteralByteString);
      CtClass googleBoundedByteString = classPool.get(protobufPackage + "ByteString$BoundedByteString");
      removePrivate(googleBoundedByteString);
      removeFinal(googleBoundedByteString);
      for (CtMethod ctMethod : googleLiteralByteString.getDeclaredMethods()) {
        removeFinal(ctMethod);
      }
      byteString.toClass();
      googleLiteralByteString.toClass();
      googleBoundedByteString.toClass();

      // Adding the classes back to the old path.
      CtClass literalByteString = classPool.makeClass(protobufPackage + "LiteralByteString");
      literalByteString.setSuperclass(googleLiteralByteString);
      literalByteString.toClass();
      CtClass boundedByteString = classPool.makeClass(protobufPackage + "BoundedByteString");
      boundedByteString.setSuperclass(googleBoundedByteString);
      boundedByteString.toClass();
    } catch (Exception e) {
      logger.warn("Unable to patch Protobuf.", e);
    }
  }

  /**
   * MapR-DB client extends {@link com.google.protobuf.GeneratedMessageLite} and overrides some methods,
   * that were made final in version 3.6+ of protobuf.
   * This method removes the final modifiers.
   */
  private static void patchGeneratedMessageLite() {
    try {
      ClassPool classPool = getClassPool();
      CtClass generatedMessageLite = classPool.get(protobufPackage + "GeneratedMessageLite");
      removeFinal(generatedMessageLite.getDeclaredMethod("getParserForType"));
      removeFinal(generatedMessageLite.getDeclaredMethod("isInitialized"));

      // The method was removed, but it is used in com.mapr.fs.proto.Dbserver.
      // Adding it back.
      generatedMessageLite.addMethod(CtNewMethod.make("protected void makeExtensionsImmutable() { }", generatedMessageLite));

      // A constructor with this signature was removed. Adding it back.
      String className = protobufPackage + "GeneratedMessageLite.Builder";
      generatedMessageLite.addConstructor(CtNewConstructor.make("protected GeneratedMessageLite(" + className + " builder) { }", generatedMessageLite));

      // This single method was added instead of several abstract methods.
      // MapR-DB client doesn't use it, but it was added in overridden equals() method.
      // Adding default implementation.
      CtMethod dynamicMethod = generatedMessageLite.getDeclaredMethod("dynamicMethod", new CtClass[]{
          classPool.get(protobufPackage + "GeneratedMessageLite$MethodToInvoke"),
          classPool.get("java.lang.Object"),
          classPool.get("java.lang.Object")});
      className = protobufPackage + "GeneratedMessageLite.MethodToInvoke";
      String dynamicMethodBody = MessageFormat.format("if ($1.equals({0}.GET_DEFAULT_INSTANCE)) '{'" +
          "  return this;" +
          "'}' else if ($1.equals({0}.BUILD_MESSAGE_INFO)) '{' " +
          "  {1}StructuralMessageInfo.Builder builder = {1}StructuralMessageInfo.newBuilder();" +
          "  builder.withSyntax({1}ProtoSyntax.PROTO2);" +
          "  builder.withDefaultInstance(this);" +
          "  return builder.build();" +
          "'}' else '{'" +
          "  return null;" +
          "'}'", className, protobufPackage);
      addImplementation(dynamicMethod, dynamicMethodBody);
      generatedMessageLite.toClass();
    } catch (Exception e) {
      logger.warn("Unable to patch Protobuf.", e);
    }
  }

  /**
   * MapR-DB client extends {@link com.google.protobuf.GeneratedMessageLite.Builder} and overrides some methods,
   * that were made final in version 3.6+ of protobuf.
   * This method removes the final modifiers.
   * Also, adding back a default constructor that was removed.
   */
  private static void patchGeneratedMessageLiteBuilder() {
    try {
      ClassPool classPool = getClassPool();
      CtClass builder = classPool.get(protobufPackage + "GeneratedMessageLite$Builder");
      removeFinal(builder.getDeclaredMethod("isInitialized"));
      removeFinal(builder.getDeclaredMethod("clear"));
      builder.addConstructor(CtNewConstructor.defaultConstructor(builder));
      builder.toClass();
    } catch (Exception e) {
      logger.warn("Unable to patch Protobuf.", e);
    }
  }

  /**
   * Removes final modifier from a given method.
   *
   * @param ctMethod method which need to be non-final.
   */
  private static void removeFinal(CtMethod ctMethod) {
    int modifiers = Modifier.clear(ctMethod.getModifiers(), Modifier.FINAL);
    ctMethod.setModifiers(modifiers);
  }

  /**
   * Removes final modifier from a given class.
   *
   * @param ctClass method which need to be non-final.
   */
  private static void removeFinal(CtClass ctClass) {
    int modifiers = Modifier.clear(ctClass.getModifiers(), Modifier.FINAL);
    ctClass.setModifiers(modifiers);
  }

  /**
   * Removes private modifier from a given class
   *
   * @param ctClass class which need to be non-private.
   */
  private static void removePrivate(CtClass ctClass) {
    int modifiers = Modifier.clear(ctClass.getModifiers(), Modifier.PRIVATE);
    ctClass.setModifiers(modifiers);
  }

  /**
   * Removes abstract modifier and adds implementation to a given method.
   *
   * @param ctMethod   method to process.
   * @param methodBody method implementation.
   * @throws CannotCompileException if unable to compile given method body.
   */
  private static void addImplementation(CtMethod ctMethod, String methodBody) throws CannotCompileException {
    ctMethod.setBody(methodBody);
    int modifiers = Modifier.clear(ctMethod.getModifiers(), Modifier.ABSTRACT);
    ctMethod.setModifiers(modifiers);
  }

  /**
   * Returns {@link javassist.scopedpool.ScopedClassPool} instance which uses the same class loader
   * which was used for loading current class.
   *
   * @return {@link javassist.scopedpool.ScopedClassPool} instance
   */
  private static ClassPool getClassPool() {
    ScopedClassPoolRepository classPoolRepository = ScopedClassPoolRepositoryImpl.getInstance();
    // sets prune flag to false to avoid freezing and pruning classes right after obtaining CtClass instance
    classPoolRepository.setPrune(false);
    return classPoolRepository.createScopedClassPool(ProtobufPatcher.class.getClassLoader(), null);
  }
}
