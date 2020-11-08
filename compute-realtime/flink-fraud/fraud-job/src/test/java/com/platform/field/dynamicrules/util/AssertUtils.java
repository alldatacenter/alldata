package com.platform.field.dynamicrules.util;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.flink.api.common.state.BroadcastState;
import org.junit.Assert;public class AssertUtils {public static <K, V> void assertEquals(
      BroadcastState<K, V> broadcastState, Map<K, V> expectedState, String message)
      throws Exception {Map<K, V> broadcastStateMap = new HashMap<>();
    for (Entry<K, V> entry : broadcastState.entries()) {
      broadcastStateMap.put(entry.getKey(), entry.getValue());
    }
    Assert.assertEquals(message, broadcastStateMap, expectedState);
  }
}
