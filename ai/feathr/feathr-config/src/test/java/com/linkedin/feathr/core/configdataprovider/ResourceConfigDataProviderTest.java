package com.linkedin.feathr.core.configdataprovider;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Unit tests for {@link ResourceConfigDataProvider}
 */
public class ResourceConfigDataProviderTest {

  @Test(description = "Tests with a single resource file")
  public void testWithSingleResource() {
    String resource = "Foo.txt";

    try (ConfigDataProvider cdp = new ResourceConfigDataProvider(resource)) {
      List<BufferedReader> readers = cdp.getConfigDataReaders()
          .stream()
          .map(BufferedReader::new)
          .collect(Collectors.toList());

      assertEquals(readers.size(), 1);
      Stream<String> stringStream = readers.get(0).lines();
      assertEquals(stringStream.count(), 3L);
    } catch (Exception e) {
      fail("Test failed", e);
    }
  }

  @Test(description = "Tests with 2 resource files")
  public void testWithMultipleResources() {
    List<String> resources = Arrays.asList("Foo.txt", "Bar.txt");

    try (ConfigDataProvider cdp = new ResourceConfigDataProvider(resources)) {
      List<BufferedReader> readers = cdp.getConfigDataReaders()
          .stream()
          .map(BufferedReader::new)
          .collect(Collectors.toList());

      assertEquals(readers.size(), resources.size());

      Stream<String> stringStream1 = readers.get(0).lines();
      assertEquals(stringStream1.count(), 3L);

      Stream<String> stringStream2 = readers.get(1).lines();
      assertEquals(stringStream2.count(), 2L);
    } catch (Exception e) {
      fail("Test failed", e);
    }
  }

  @Test(description = "Tests custom class loader")
  public void testCustomClassLoader() {
    String resource = "Foo.txt";

    try (ConfigDataProvider cdp =
        new ResourceConfigDataProvider(resource, Thread.currentThread().getContextClassLoader())) {
      List<BufferedReader> readers =
          cdp.getConfigDataReaders().stream().map(BufferedReader::new).collect(Collectors.toList());

      assertEquals(readers.size(), 1);
      Stream<String> stringStream = readers.get(0).lines();
      assertEquals(stringStream.count(), 3L);
    } catch (Exception e) {
      fail("Test failed", e);
    }
  }
}
