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

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * This is a {@link org.junit.rules.TestWatcher} which is used to create and delete sub directories before and after unit tests respectively.
 * Ideally a {@link SubDirTestWatcher} would be used in conjunction with a {@link DirTestWatcher} or a sub class of {@link DirTestWatcher}. The
 * {@link DirTestWatcher} would be used to create the base directory used by the {@link SubDirTestWatcher} to create all its sub directories. Here
 * is an example of using the {@link SubDirTestWatcher} in this way.
 *
 * <pre>
 * package my.proj;
 *
 * public class MyTestClass {
 *   public static final String TEST_SUB_DIR = "testSubDir";
 *   private static File testSubDir;
 *
 *   &#064;org.junit.ClassRule
 *   public static final DirTestWatcher dirTestWatcher = new DirTestWatcher();
 *
 *   &#064;org.junit.BeforeClass
 *   public static void setupFiles() {
 *     testSubDir = new File(dirTestWatcher.getDir(), TEST_SUB_DIR);
 *   }
 *
 *   &#064;org.junit.Rule
 *   public final SubDirTestWatcher subDirTestWatcher =
 *     new SubDirTestWatcher.Builder(dirTestWatcher.getDir())
 *       .addSubDir(TEST_SUB_DIR)
 *       .build();
 *
 *   &#064;org.junit.Test
 *   public void myTestMethod1() {
 *     // The testSubDir should be created for us by the SubDirTestWatcher
 *     org.junit.Assert.assertTrue(testSubDir.exists());
 *
 *     // Lets make another sub directory in our sub directory
 *     File newSubDirectory = new File(testSubDir, "a");
 *     newSubDirectory.mkdirs();
 *     org.junit.Assert.assertTrue(newSubDirectory.exists());
 *   }
 *
 *   &#064;org.junit.Test
 *   public void myTestMethod2() {
 *     // The testSubDir should be created for us by the SubDirTestWatcher
 *     org.junit.Assert.assertTrue(testSubDir.exists());
 *
 *     // The directory we created in the previous test should be gone
 *     org.junit.Assert.assertFalse(new File(testSubDir, "a").exists());
 *   }
 * }
 * </pre>
 */
public class SubDirTestWatcher extends TestWatcher {
  private File baseDir;
  private boolean createAtBeginning;
  private boolean deleteAtEnd;
  private List<Path> subDirs;

  protected SubDirTestWatcher(File baseDir, boolean createAtBeginning, boolean deleteAtEnd, List<Path> subDirs) {
    this.baseDir = Preconditions.checkNotNull(baseDir);
    this.createAtBeginning = createAtBeginning;
    this.deleteAtEnd = deleteAtEnd;
    this.subDirs = Preconditions.checkNotNull(subDirs);

    Preconditions.checkArgument(!subDirs.isEmpty(), "The list of subDirs is empty.");
  }

  @Override
  protected void starting(Description description) {
    deleteDirs(true);

    if (!createAtBeginning) {
      return;
    }

    for (Path subDir: subDirs) {
      baseDir.toPath()
        .resolve(subDir)
        .toFile()
        .mkdirs();
    }
  }

  @Override
  protected void finished(Description description) {
    deleteDirs(deleteAtEnd);
  }

  @Override
  protected void failed(Throwable e, Description description) {
    deleteDirs(deleteAtEnd);
  }

  private void deleteDirs(boolean delete) {
    if (!delete) {
      return;
    }

    for (Path subDir: subDirs) {
      FileUtils.deleteQuietly(baseDir.toPath().resolve(subDir).toFile());
    }
  }

  /**
   * This is a builder for a {@link SubDirTestWatcher}. Create an instance of the builder, configure it, and then call the {@link Builder#build()} to create a
   * {@link SubDirTestWatcher}.
   */
  public static class Builder {
    private File baseDir;
    private boolean createAtBeginning = true;
    private boolean deleteAtEnd = true;
    private List<Path> subDirs = Lists.newArrayList();

    /**
     * Initializes a builder for a {@link SubDirTestWatcher} with the given baseDir.
     * @param baseDir The baseDir is the parent directory in which all sub directories are created.
     */
    public Builder(File baseDir) {
      this.baseDir = Preconditions.checkNotNull(baseDir);
    }

    /**
     * Sets the createAtBeginning flag.
     * @param createAtBeginning This flag determines whether the {@link SubDirTestWatcher} creates the configured sub directories at a beginning of a test. Setting
     *                          This to true will cause the {@link SubDirTestWatcher} to create the configured sub directories at the beginning of each test. Setting
     *                          This to false will prevent the {@link SubDirTestWatcher} from attempting to create the configured sub directories at the beginning of
     *                          at test. Setting this to false is useful for scenarios where you only want to delete directories created in the middle of your tests. By
     *                          default this flag is true.
     * @return The {@link SubDirTestWatcher.Builder}.
     */
    public Builder setCreateAtBeginning(boolean createAtBeginning) {
      this.createAtBeginning = createAtBeginning;
      return this;
    }

    /**
     * Sets the deleteAtEnd flag.
     * @param deleteAtEnd This flag determines whether the {@link SubDirTestWatcher} deletes the sub directories it manages at the end of a test.
     * @return The {@link SubDirTestWatcher.Builder}.
     */
    public Builder setDeleteAtEnd(boolean deleteAtEnd) {
      this.deleteAtEnd = deleteAtEnd;
      return this;
    }

    /**
     * Adds a sub directory to be managed by the {@link SubDirTestWatcher}.
     * @param subDir The relative path of the sub directory to be added.
     * @return The {@link SubDirTestWatcher.Builder}.
     */
    public Builder addSubDir(Path subDir) {
      Preconditions.checkNotNull(subDir);
      subDirs.add(subDir);
      return this;
    }

    /**
     * Builds a {@link SubDirTestWatcher}.
     * @return A configured {@link SubDirTestWatcher}.
     */
    public SubDirTestWatcher build() {
      Preconditions.checkState(!subDirs.isEmpty(), "The list of subDirs is empty.");
      return new SubDirTestWatcher(baseDir, createAtBeginning, deleteAtEnd, subDirs);
    }
  }
}
