package com.linkedin.feathr.core.config.producer.features;

import java.util.Optional;

/**
 * Denotes availability of a feature in a particular environment.
 */
public enum Availability {
  OFFLINE,
  ONLINE,
  OFFLINE_ONLINE;

  public static Optional<Availability> fromName(String name) {
    Availability res = null;

    for (Availability a : values()) {
      if (a.name().equalsIgnoreCase(name)) {
        res = a;
        break;
      }
    }

    return Optional.ofNullable(res);
  }
}
