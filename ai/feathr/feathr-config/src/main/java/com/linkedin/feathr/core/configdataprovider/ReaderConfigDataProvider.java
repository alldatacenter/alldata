package com.linkedin.feathr.core.configdataprovider;

import com.linkedin.feathr.core.configbuilder.ConfigBuilder;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * A Config Data Provider that obtains config data from Reader objects. It merely exposes the same Reader objects
 * to its clients, and is provided for consistent usage of
 * {@link ConfigBuilder ConfigBuilder} API.
 */
public class ReaderConfigDataProvider extends BaseConfigDataProvider {

  public ReaderConfigDataProvider(Reader reader) {
    this(Collections.singletonList(reader));
  }

  public ReaderConfigDataProvider(List<Reader> readers) {
    Objects.requireNonNull(readers, "List of Readers can't be null");
    for (Reader r : readers) {
      Objects.requireNonNull(r, "A Reader object can't be null");
    }
    _readers = readers;
  }

  @Override
  public List<Reader> getConfigDataReaders() {
    return _readers;
  }

  @Override
  public String getConfigDataInfo() {
    return "Reader object(s)";
  }
}
