package com.netease.arctic.server.persistence;

import com.netease.arctic.optimizing.RewriteFilesInput;
import com.netease.arctic.optimizing.RewriteFilesOutput;
import com.netease.arctic.server.optimizing.TaskRuntime;
import com.netease.arctic.server.persistence.mapper.OptimizingMapper;
import com.netease.arctic.server.utils.CompressUtil;
import com.netease.arctic.utils.SerializationUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskFilesPersistence {

  private static final DatabasePersistence persistence = new DatabasePersistence();

  public static void persistTaskInputs(long processId, Collection<TaskRuntime> tasks) {
    persistence.persistTaskInputs(processId, tasks.stream().collect(Collectors.toMap(e -> e.getTaskId().getTaskId(),
        TaskRuntime::getInput)));
  }

  public static Map<Integer, RewriteFilesInput> loadTaskInputs(long processId) {
    List<byte[]> bytes =
        persistence.getAs(OptimizingMapper.class, mapper -> mapper.selectProcessInputFiles(processId));
    if (bytes == null) {
      return Collections.emptyMap();
    } else {
      return SerializationUtil.simpleDeserialize(CompressUtil.unGzip(bytes.get(0)));
    }
  }

  public static RewriteFilesOutput loadTaskOutput(byte[] content) {
    return SerializationUtil.simpleDeserialize(content);
  }

  private static class DatabasePersistence extends PersistentBase {

    public void persistTaskInputs(long processId, Map<Integer, RewriteFilesInput> tasks) {
      doAs(OptimizingMapper.class, mapper ->
          mapper.updateProcessInputFiles(processId, tasks));
    }
  }
}
