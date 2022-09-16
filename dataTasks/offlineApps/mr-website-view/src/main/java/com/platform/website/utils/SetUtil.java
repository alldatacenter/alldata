package com.platform.website.utils;

import java.util.Set;

public class SetUtil {

  /**
   * 判断是否存在
   */
  public static boolean contains(Set<Object> set, Object o) {
    for (Object object : set) {
      if (object.equals(o)) {
        return true;
      }
      if (object instanceof Set) {
        @SuppressWarnings("unchecked")
        Set<Object> innerSet = (Set<Object>) object;
        for (Object obj : innerSet) {
          if (obj.equals(o)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
