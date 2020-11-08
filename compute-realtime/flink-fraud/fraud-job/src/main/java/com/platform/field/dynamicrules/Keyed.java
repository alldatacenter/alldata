package com.platform.field.dynamicrules;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;@Data
@NoArgsConstructor
@AllArgsConstructor
public class Keyed<IN, KEY, ID> {
  private IN wrapped;
  private KEY key;
  private ID id;
}
