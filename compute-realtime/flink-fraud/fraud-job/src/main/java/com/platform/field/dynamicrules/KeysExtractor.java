package com.platform.field.dynamicrules;
import java.util.Iterator;
import java.util.List;/** Utilities for dynamic keys extraction by field name. */
public class KeysExtractor {/** Extracts and concatenates field values by names.
   *
   * @param keyNames list of field names
   * @param object target for values extraction
   */
  public static String getKey(List<String> keyNames, Object object)
      throws NoSuchFieldException, IllegalAccessException {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (keyNames.size() > 0) {
      Iterator<String> it = keyNames.iterator();
      appendKeyValue(sb, object, it.next());while (it.hasNext()) {
        sb.append(";");
        appendKeyValue(sb, object, it.next());
      }
    }
    sb.append("}");
    return sb.toString();
  }private static void appendKeyValue(StringBuilder sb, Object object, String fieldName)
      throws IllegalAccessException, NoSuchFieldException {
    sb.append(fieldName);
    sb.append("=");
    sb.append(FieldsExtractor.getFieldAsString(object, fieldName));
  }
}
