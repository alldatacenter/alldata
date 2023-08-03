package com.linkedin.feathr.core.configdataprovider;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Unit tests for {@link StringConfigDataProvider}
 */
public class StringConfigDataProviderTest {

  @Test(description = "Tests with single string")
  public void testWithSingleString() {
    String line1 = "This is line 1";
    String line2 = "This is line two";
    String line3 = "This is line number 3";
    String lines = String.join("\n", line1, line2, line3);

    try (ConfigDataProvider cdp = new StringConfigDataProvider(lines)) {
      List<BufferedReader> stringReaders = cdp.getConfigDataReaders()
          .stream()
          .map(BufferedReader::new)
          .collect(Collectors.toList());

      assertEquals(stringReaders.size(), 1);

      BufferedReader strReader = stringReaders.get(0);
      assertEquals(strReader.readLine(), line1);
      assertEquals(strReader.readLine(), line2);
      assertEquals(strReader.readLine(), line3);
      assertNull(strReader.readLine());
    } catch (Exception e) {
      fail("Caught exception", e);
    }
  }

  @Test(description = "Tests with 2 strings")
  public void testWithMultipleStrings() {
    String line11 = "This is line 1";
    String line12 = "This is line two";
    String line13 = "This is line number 3";
    String str1 = String.join("\n", line11, line12, line13);

    String line21 = "There is no greatness where there is not simplicity, goodness, and truth.";
    String line22 = "The strongest of all warriors are these two — Time and Patience.";
    String str2 = String.join("\n", line21, line22);

    List<String> strings = Arrays.asList(str1, str2);

    try (ConfigDataProvider cdp = new StringConfigDataProvider(strings)) {
      List<BufferedReader> stringReaders = cdp.getConfigDataReaders()
          .stream()
          .map(BufferedReader::new)
          .collect(Collectors.toList());

      assertEquals(stringReaders.size(), strings.size());

      BufferedReader strReader1 = stringReaders.get(0);
      assertEquals(strReader1.readLine(), line11);
      assertEquals(strReader1.readLine(), line12);
      assertEquals(strReader1.readLine(), line13);
      assertNull(strReader1.readLine());

      BufferedReader strReader2 = stringReaders.get(1);
      assertEquals(strReader2.readLine(), line21);
      assertEquals(strReader2.readLine(), line22);
      assertNull(strReader2.readLine());

    } catch (Exception e) {
      fail("Caught exception", e);
    }
  }
}
