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
package org.apache.drill.exec.physical.impl.scan.v3.file;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader.EarlyEofException;
import org.apache.drill.exec.physical.impl.scan.v3.ScanFixture;
import org.apache.drill.exec.physical.impl.scan.v3.ScanFixture.ScanFixtureBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.ScanLifecycleBuilder;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.drill.test.SubOperatorTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

public class BaseFileScanTest extends SubOperatorTest implements MockFileNames {

  /**
   * Fixture that creates the reader and gives a test access to
   * the reader.
   */
  protected interface ReaderCreator {
    ManagedReader instance(FileSchemaNegotiator negotiator) throws EarlyEofException;
  }

  protected static class ReaderFactoryFixture extends FileReaderFactory {

    private final Iterator<ReaderCreator> iterator;

    public ReaderFactoryFixture(Iterator<ReaderCreator> iterator) {
      this.iterator = iterator;
    }

    @Override
    public ManagedReader newReader(FileSchemaNegotiator negotiator) throws EarlyEofException {
      return iterator.next().instance(negotiator);
    }
  }

  /**
   * For schema-based testing, we only need the file path from the file work.
   */
  public static class DummyFileWork implements FileWork {

    private final Path path;

    public DummyFileWork(Path path) {
      this.path = path;
    }

    @Override
    public Path getPath() { return path; }

    @Override
    public long getStart() { return 0; }

    @Override
    public long getLength() { return 0; }
  }

  protected static class FileScanFixtureBuilder extends ScanFixtureBuilder {

    public FileScanLifecycleBuilder builder = new FileScanLifecycleBuilder();
    public List<ReaderCreator> readers = new ArrayList<>();
    public List<FileWork> blocks = new ArrayList<>();

    public FileScanFixtureBuilder() {
      super(fixture);
      builder.rootDir(MOCK_ROOT_PATH);
      builder.maxPartitionDepth(3);
    }

    public void projectAllWithImplicit(int dirs) {
      builder().projection(FileScanUtils.projectAllWithMetadata(dirs));
    }

    public void addReader(ReaderCreator reader) {
      addReader(reader, new DummyFileWork(new Path(MOCK_FILE_SYSTEM_NAME)));
    }

    public void addReader(ReaderCreator reader, FileWork block) {
      readers.add(reader);
      blocks.add(block);
    }

    @Override
    public ScanLifecycleBuilder builder() { return builder; }

    @Override
    public ScanFixture build() {
      builder.fileSystemConfig(new Configuration());
      builder.fileSplits(blocks);
      builder.readerFactory(new ReaderFactoryFixture(readers.iterator()));
      return super.build();
    }
  }
}
