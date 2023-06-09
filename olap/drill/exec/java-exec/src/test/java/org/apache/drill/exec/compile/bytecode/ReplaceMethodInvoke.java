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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.compile.DrillCheckClassAdapter;
import org.apache.drill.exec.compile.QueryClassLoader;
import org.apache.drill.exec.planner.PhysicalPlanReaderTestFactory;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.store.sys.store.provider.LocalPersistentStoreProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;

public class ReplaceMethodInvoke {
  // private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReplaceMethodInvoke.class);

  public static void main(String[] args) throws Exception {
    final String k2 = "org/apache/drill/Pickle.class";
    final URL url = Resources.getResource(k2);
    final byte[] clazz = Resources.toByteArray(url);
    final ClassReader cr = new ClassReader(clazz);

    final ClassWriter cw = writer();
    final TraceClassVisitor visitor = new TraceClassVisitor(cw, new Textifier(), new PrintWriter(System.out));
    final ValueHolderReplacementVisitor v2 = new ValueHolderReplacementVisitor(visitor, true);
    cr.accept(v2, ClassReader.EXPAND_FRAMES );//| ClassReader.SKIP_DEBUG);

    final byte[] output = cw.toByteArray();
    Files.write(output, new File("/src/scratch/bytes/S.class"));
    check(output);

    final DrillConfig c = DrillConfig.forClient();
    final SystemOptionManager m = new SystemOptionManager(PhysicalPlanReaderTestFactory.defaultLogicalPlanPersistence(c), new LocalPersistentStoreProvider(c), c);
    m.init();
    try (QueryClassLoader ql = new QueryClassLoader(DrillConfig.create(), m)) {
      ql.injectByteCode("org.apache.drill.Pickle$OutgoingBatch", output);
      Class<?> clz = ql.loadClass("org.apache.drill.Pickle$OutgoingBatch");
      clz.getMethod("x").invoke(null);
    }
  }

  private static final void check(final byte[] b) {
    final ClassReader cr = new ClassReader(b);
    final ClassWriter cw = writer();
    final ClassVisitor cv = new DrillCheckClassAdapter(cw);
    cr.accept(cv, 0);

    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    DrillCheckClassAdapter.verify(new ClassReader(cw.toByteArray()), false, pw);

    final String checkString = sw.toString();
    if (!checkString.isEmpty()) {
      throw new IllegalStateException(checkString);
    }
  }

  private static ClassWriter writer() {
    final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    return cw;
  }
}
