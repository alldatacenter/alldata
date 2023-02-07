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
package org.apache.drill.test;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>
 * This class is used to create consistently named and safe temp directories for
 * unit tests.
 * </p>
 * <p>
 * A {@link DirTestWatcher} is added to a test by declaring it as a JUnit
 * {@link org.junit.Rule}. A {@link org.junit.Rule Rule} is a piece of code that
 * is run before and after every JUnit test marked with the
 * {@link org.junit.Test Test} annotation. When the {@link DirTestWatcher} is
 * added to a test class the {@link DirTestWatcher} will create a temp directory
 * before each of your {@link org.junit.Test Test}s and optionally delete the
 * temp directory after each of your {@link org.junit.Test Test}s. The <b>base
 * temp directory</b> created by the {@link DirTestWatcher} is in the
 * <b>target</b> folder of the maven project and has the form <b>(my test class
 * fully qualified name)/(my test method name)</b>. So in the context of the
 * code example below, the temp directory created for each test in <b>target</b>
 * will be <b>my.proj.MyTestClass/myTestMethod1</b> and
 * <b>my.proj.MyTestClass/myTestMethod2</b> respectively.
 * </p>
 * <p>
 * The temp directory created by the {@link DirTestWatcher} can be used within a
 * test by simply calling the {@link DirTestWatcher#getDir()} method on the
 * {@link DirTestWatcher} within your unit test.
 * </p>
 *
 * <p>
 * By default, the {@link DirTestWatcher} deletes the temp directory it creates
 * at the end of each {@link org.junit.Test Test}. However, you can create a
 * {@link DirTestWatcher} by doing {@code new DirTestWatcher(false)} to disable
 * the deletion of temp directories after a test. This is useful if you want to
 * examine files after a test runs.
 * </p>
 *
 * <pre>
 * <code>
 * package my.proj;
 *
 * public class MyTestClass {
 *   &#064;Rule
 *   public final DirTestWatcher dirTestWatcher = new DirTestWatcher();
 *
 *   &#064;Test
 *   public void myTestMethod1() {
 *     File dir = dirTestWatcher.getDir();
 *     // Do stuff in the temp directory
 *   }
 *
 *   &#064;Test
 *   public void myTestMethod2() {
 *     File dir = dirTestWatcher.getDir();
 *     // Do stuff in the temp directory
 *   }
 * }
 * </code>
 * </pre>
 *
 * <p>
 * <b>Note:</b> In the code sample above, the directories returned by
 * {@link DirTestWatcher#getDir()} in myTestMethod1 and myTestMethod2 are
 * <b>my.proj.MyTestClass/myTestMethod1</b> and
 * <b>my.proj.MyTestClass/myTestMethod2</b> respectively.
 * TODO: need to implement {@link AfterEachCallback} and {@link BeforeEachCallback} to use it with
 *  JUnit5 @ExtendWith annotation
 * </p>
 */
public class DirTestWatcher extends TestWatcher {
  private static final int TEMP_DIR_ATTEMPTS = 10000;

  private String dirPath;
  private File dir;
  private boolean deleteDirAtEnd = true;

  /**
   * Creates a {@link DirTestWatcher} that deletes temp directories after the
   * {@link TestWatcher} completes.
   */
  public DirTestWatcher() {
  }

  /**
   * Creates a {@link DirTestWatcher} which can delete or keep the temp
   * directory after the {@link TestWatcher} completes.
   *
   * @param deleteDirAtEnd
   *          When true the temp directory created by the {@link DirTestWatcher}
   *          is deleted. When false the temp directory created by the
   *          {@link DirTestWatcher} is not deleted.
   */
  public DirTestWatcher(boolean deleteDirAtEnd) {
    this.deleteDirAtEnd = deleteDirAtEnd;
  }

  @Override
  protected void starting(Description description) {
    if (description.getMethodName() != null) {
      dirPath = Paths.get(".", "target", description.getClassName(),
          description.getMethodName()).toString();
    } else {
      dirPath = Paths.get(".", "target", description.getClassName()).toString();
    }

    dir = new File(dirPath);
    FileUtils.deleteQuietly(dir);
    dir.mkdirs();
  }

  @Override
  protected void finished(Description description) {
    deleteDir();
  }

  @Override
  protected void failed(Throwable e, Description description) {
    deleteDir();
  }

  /**
   * Creates a sub directory with the given relative path in current temp
   * directory
   *
   * @param relativeDirPath
   *          The relative path of the sub directory to create in the current
   *          temp directory.
   * @return The {@link java.io.File} object of the newly created sub directory.
   */
  public File makeSubDir(Path relativeDirPath) {
    File subDir = dir.toPath().resolve(relativeDirPath).toFile();
    subDir.mkdirs();
    return subDir;
  }

  private void deleteDir() {
    if (deleteDirAtEnd) {
      FileUtils.deleteQuietly(dir);
    }
  }

  /**
   * Gets the {@link java.io.File} object of the current temp directory.
   *
   * @return The {@link java.io.File} object of the current temp directory.
   */
  public File getDir() {
    return dir;
  }

  public static File createTempDir(File baseDir) {
    String baseName = System.currentTimeMillis() + "-";

    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
      File tempDir = new File(baseDir, baseName + counter);
      if (tempDir.mkdirs()) {
        return tempDir;
      }
    }

    String message = String.format(
        "Failed to create directory within %s attempts (tried %s0 to %s)",
        TEMP_DIR_ATTEMPTS, baseName, baseName + (TEMP_DIR_ATTEMPTS - 1));
    throw new IllegalStateException(message);
  }
}
