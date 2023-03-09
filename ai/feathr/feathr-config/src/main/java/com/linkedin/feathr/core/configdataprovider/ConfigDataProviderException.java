package com.linkedin.feathr.core.configdataprovider;

/**
 * Runtime Exception thrown by a {@link ConfigDataProvider} object when an error is encountered in fetching config data.
 */
public class ConfigDataProviderException extends RuntimeException {
  public ConfigDataProviderException(String message) {
    super(message);
  }

  public ConfigDataProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
