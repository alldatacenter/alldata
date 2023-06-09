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

import java.util.Iterator;

import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader.EarlyEofException;
import org.apache.drill.exec.physical.impl.scan.v3.ReaderFactory;
import org.apache.drill.exec.store.dfs.easy.FileWork;

/**
 * Iterates over the splits for the present scan. For each, creates a
 * new reader. The file framework passes the file split (and the Drill
 * file system) in via the schema negotiator to the constructor which
 * should open the file.
 */
public abstract class FileReaderFactory implements ReaderFactory<FileSchemaNegotiator> {
  private Iterator<FileWork> splitIter;

  protected void bind(FileScanLifecycle scan) {
    this.splitIter = scan.fileScanOptions().splits().iterator();
  }

  @Override
  public boolean hasNext() { return splitIter.hasNext(); }

  @Override
  public ManagedReader next(FileSchemaNegotiator negotiator) throws EarlyEofException {
    FileSchemaNegotiatorImpl negotiatorImpl = (FileSchemaNegotiatorImpl) negotiator;
    negotiatorImpl.bindSplit(splitIter.next());
    return newReader(negotiator);
  }

  public abstract ManagedReader newReader(FileSchemaNegotiator negotiator) throws EarlyEofException;
}
