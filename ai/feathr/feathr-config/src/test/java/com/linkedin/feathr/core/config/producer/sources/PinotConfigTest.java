package com.linkedin.feathr.core.config.producer.sources;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.testng.annotations.Test;

/**
 * Test class for {@link PinotConfig}
 */
public class PinotConfigTest {
  @Test(description = "test equals and hashcode")
  public void testEqualsHashcode() {
    EqualsVerifier.forClass(PinotConfig.class).usingGetClass().verify();
  }
}
