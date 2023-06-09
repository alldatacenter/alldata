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
package org.apache.drill.exec.physical.impl.scan.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.drill.common.exceptions.ChildErrorContext;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.exceptions.UserException.Builder;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorEvents;
import org.apache.drill.exec.physical.impl.scan.file.ImplicitColumnManager.ImplicitColumnOptions;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiatorImpl;
import org.apache.drill.exec.physical.impl.scan.framework.ShimBatchReader;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The file scan framework adds into the scan framework support for implicit
 * reading from DFS splits (a file and a block). Since this framework is
 * file-based, it also adds support for file metadata (AKA implicit columns.
 * The file scan framework brings together a number of components:
 * <ul>
 * <li>The set of options defined by the base framework.</li>
 * <li>The set of files and/or blocks to read.</li>
 * <li>The file system configuration to use for working with the files
 * or blocks.</li>
 * <li>The factory class to create a reader for each of the files or blocks
 * defined above. (Readers are created one-by-one as files are read.)</li>
 * <li>Options as defined by the base class.</li>
 * </ul>
 * <p>
 * The framework iterates over file descriptions, creating readers at the
 * moment they are needed. This allows simpler logic because, at the point of
 * reader creation, we have a file system, context and so on.
 * <p>
 * @See {AbstractScanFramework} for details.
 */
public class FileScanFramework extends ManagedScanFramework {
  private static final Logger logger = LoggerFactory.getLogger(FileScanFramework.class);

  /**
   * The file schema negotiator adds no behavior at present, but is
   * created as a placeholder anticipating the need for file-specific
   * behavior later. Readers are expected to use an instance of this
   * class so that their code need not change later if/when we add new
   * methods. For example, perhaps we want to specify an assumed block
   * size for S3 files, or want to specify behavior if the file no longer
   * exists. Those are out of scope of this first round of changes which
   * focus on schema.
   */
  public interface FileSchemaNegotiator extends SchemaNegotiator {

    /**
     * Gives the Drill file system for this operator.
     */
    DrillFileSystem fileSystem();

    /**
     * Describes the file split (path and block offset) for this scan.
     *
     * @return Hadoop file split object with the file path, block
     * offset, and length.
     */
    FileSplit split();
  }

  /**
   * Implementation of the file-level schema negotiator. At present, no
   * file-specific features exist. This class shows, however, where we would
   * add such features.
   */
  public static class FileSchemaNegotiatorImpl extends SchemaNegotiatorImpl
      implements FileSchemaNegotiator {

    private final FileSplit split;

    public FileSchemaNegotiatorImpl(FileScanFramework framework) {
      super(framework);
      this.split = framework.currentSplit;
      context = new FileRowSetContext(parentErrorContext(), split);
    }

    @Override
    public DrillFileSystem fileSystem() {
      return ((FileScanFramework) framework).dfs;
    }

    @Override
    public FileSplit split() { return split; }
  }

  public static class FileRowSetContext extends ChildErrorContext {

    private final FileSplit split;

    public FileRowSetContext(CustomErrorContext parent, FileSplit split) {
      super(parent);
      this.split = split;
    }

    @Override
    public void addContext(Builder builder) {
      super.addContext(builder);
      builder.addContext("File:", Path.getPathWithoutSchemeAndAuthority(split.getPath()).toString());
      if (split.getStart() != 0) {
        builder.addContext("Offset:", split.getStart());
      }
    }
  }

  /**
   * Options for a file-based scan.
   */
  public static class FileScanBuilder extends ScanFrameworkBuilder {
    private List<? extends FileWork> files;
    private Configuration fsConf;
    private final ImplicitColumnOptions metadataOptions = new ImplicitColumnOptions();

    public void setFileSystemConfig(Configuration fsConf) {
      this.fsConf = fsConf;
    }

    public void setFiles(List<? extends FileWork> files) {
      this.files = files;
    }

    public ImplicitColumnOptions implicitColumnOptions() { return metadataOptions; }

    @Override
    public ScanOperatorEvents buildEvents() {
      return new FileScanFramework(this);
    }
  }

  /**
   * Iterates over the splits for the present scan. For each, creates a
   * new reader. The file framework passes the file split (and the Drill
   * file system) in via the schema negotiator at open time. This protocol
   * makes clear that the constructor for the reader should do nothing;
   * work should be done in the open() call.
   */
  public abstract static class FileReaderFactory implements ReaderFactory {

    private FileScanFramework fileFramework;

    @Override
    public void bind(ManagedScanFramework baseFramework) {
      this.fileFramework = (FileScanFramework) baseFramework;
    }

    @Override
    public ManagedReader<? extends SchemaNegotiator> next() {
      if (fileFramework.nextSplit() == null) {
        return null;
      }
      return newReader();
    }

    public CustomErrorContext errorContext() {
      return fileFramework == null ? null : fileFramework.errorContext();
    }

    public abstract ManagedReader<? extends FileSchemaNegotiator> newReader();

    /**
     * @return FileScanFramework or empty object in case it is not binded yet with {@link #bind(ManagedScanFramework)}
     */
    protected Optional<FileScanFramework> fileFramework() {
      return Optional.ofNullable(fileFramework);
    }
  }

  private ImplicitColumnManager metadataManager;
  private DrillFileSystem dfs;
  private final List<FileSplit> splits = new ArrayList<>();
  private Iterator<FileSplit> splitIter;
  private FileSplit currentSplit;

  public FileScanFramework(FileScanBuilder builder) {
    super(builder);
    assert builder.files != null;
    assert builder.fsConf != null;
  }

  public FileScanBuilder options() {
    return (FileScanBuilder) builder;
  }

  @Override
  protected void configure() {
    super.configure();
    FileScanBuilder options = options();

    // Create the Drill file system.

    try {
      dfs = context.newFileSystem(options.fsConf);
    } catch (IOException e) {
      throw UserException.dataReadError(e)
        .addContext("Failed to create FileSystem")
        .build(logger);
    }

    // Prepare the list of files. We need the list of paths up
    // front to compute the maximum partition. Then, we need to
    // iterate over the splits to create readers on demand.

    List<Path> paths = new ArrayList<>();
    for (FileWork work : options.files) {
      Path path = dfs.makeQualified(work.getPath());
      paths.add(path);
      FileSplit split = new FileSplit(path, work.getStart(), work.getLength(), new String[]{""});
      splits.add(split);
    }
    splitIter = splits.iterator();

    // Create the metadata manager to handle file metadata columns
    // (so-called implicit columns and partition columns.)
    options.implicitColumnOptions().setFiles(paths);
    metadataManager = new ImplicitColumnManager(
        context.getFragmentContext().getOptions(),
        options.implicitColumnOptions(),
        dfs);
    builder.withImplicitColumns(metadataManager);
  }

  protected FileSplit nextSplit() {
    if (! splitIter.hasNext()) {
      currentSplit = null;
      return null;
    }
    currentSplit = splitIter.next();

    // Tell the metadata manager about the current file so it can
    // populate the metadata columns, if requested.
    metadataManager.startFile(currentSplit.getPath());
    return currentSplit;
  }

  @Override
  protected SchemaNegotiatorImpl newNegotiator() {
    return new FileSchemaNegotiatorImpl(this);
  }

  @Override
  public boolean open(ShimBatchReader shimBatchReader) {
    try {
      return super.open(shimBatchReader);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      throw UserException.executionError(e)
        .addContext("File", currentSplit.getPath().toString())
        .build(logger);
    }
  }

  public DrillFileSystem fileSystem() {
    return dfs;
  }
}
