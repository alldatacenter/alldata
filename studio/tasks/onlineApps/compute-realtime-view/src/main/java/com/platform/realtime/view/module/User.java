package com.platform.realtime.view.module;

import java.io.Serializable;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 对应数据库的module
 */
@Data
@Component
public class User implements Serializable {
  private Integer id;
  private String name;
  private Integer age;
}
