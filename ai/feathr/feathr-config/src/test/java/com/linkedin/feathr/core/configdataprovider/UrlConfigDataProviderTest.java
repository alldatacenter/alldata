package com.linkedin.feathr.core.configdataprovider;

import java.io.BufferedReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Unit tests for {@link UrlConfigDataProvider}
 */
public class UrlConfigDataProviderTest {
  private static ClassLoader _classLoader;

  @BeforeClass
  public static void init() {
    _classLoader = Thread.currentThread().getContextClassLoader();
  }

  @Test(description = "Tests with a single URL")
  public void testWithSingleUrl() {
    String resource = "Foo.txt";
    URL url = _classLoader.getResource(resource);

    try (ConfigDataProvider cdp = new UrlConfigDataProvider(url)) {
      List<BufferedReader> readers = cdp.getConfigDataReaders()
          .stream()
          .map(BufferedReader::new)
          .collect(Collectors.toList());

      assertEquals(readers.size(), 1);
      Stream<String> stringStream = readers.get(0).lines();
      assertEquals(stringStream.count(), 3L);
    } catch (Exception e) {
      fail("Caught exception", e);
    }
  }

  @Test(description = "Tests with two URLs")
  public void testWithMultipleUrls() {
    List<String> resources = Arrays.asList("Foo.txt", "Bar.txt");
    List<URL> urls = resources.stream().map(r -> _classLoader.getResource(r)).collect(Collectors.toList());

    try (ConfigDataProvider cdp = new UrlConfigDataProvider(urls)) {
      List<BufferedReader> readers = cdp.getConfigDataReaders()
          .stream()
          .map(BufferedReader::new)
          .collect(Collectors.toList());

      assertEquals(readers.size(), urls.size());

      Stream<String> stringStream1 = readers.get(0).lines();
      assertEquals(stringStream1.count(), 3L);

      Stream<String> stringStream2 = readers.get(1).lines();
      assertEquals(stringStream2.count(), 2L);
    } catch (Exception e) {
      fail("Caught exception", e);
    }

  }
}
