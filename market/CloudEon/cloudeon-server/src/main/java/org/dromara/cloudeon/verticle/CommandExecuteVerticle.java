package org.dromara.cloudeon.verticle;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.cloudeon.dao.CommandRepository;
import org.dromara.cloudeon.dao.CommandTaskRepository;
import org.dromara.cloudeon.entity.CommandEntity;
import org.dromara.cloudeon.entity.CommandTaskEntity;
import org.dromara.cloudeon.enums.CommandState;
import org.dromara.cloudeon.processor.BaseCloudeonTask;
import org.dromara.cloudeon.processor.TaskParam;
import org.dromara.cloudeon.utils.Constant;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class CommandExecuteVerticle extends AbstractVerticle {


    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer(Constant.VERTX_COMMAND_ADDRESS, message -> {
            Integer commandId = (Integer) message.body();
            // 1. 从message中获取command
            vertx.executeBlocking(future -> {
                onReceiveCommandId(commandId);
            }, false, res -> {
                log.info("CommandExecuteVerticle commandId:{} 执行完毕", commandId);
            });
        });
    }


    /**
     * 处理请求
     */
    private void onReceiveCommandId(Integer commandId) {
        log.info("CommandExecuteActor 接收到指令id为" + commandId + "...........");
        CommandTaskRepository commandTaskRepository = SpringUtil.getBean(CommandTaskRepository.class);
        CommandRepository commandRepository = SpringUtil.getBean(CommandRepository.class);

        List<CommandTaskEntity> taskEntityList = null;
        // 解决扫描不到任务直接结束command的问题
        while (true) {
            taskEntityList = commandTaskRepository.findByCommandId(commandId);
            int size = taskEntityList.size();
            log.info("根据commandId {} 找出task数量：{}", commandId, size);
            if (size > 0) break;
            ThreadUtil.sleep(2_000);
        }


        // 根据任务列表生成runnable
        List<Runnable> runnableList = taskEntityList.stream().map(new Function<CommandTaskEntity, Runnable>() {
            @Override
            public Runnable apply(CommandTaskEntity commandTaskEntity) {
                // 反射生成任务对象
                BaseCloudeonTask o = ReflectUtil.newInstance(commandTaskEntity.getProcessorClassName());
                // 填充任务参数
                o.setTaskParam(JSONObject.parseObject(commandTaskEntity.getTaskParam(), TaskParam.class));
                return o;
            }
        }).collect(Collectors.toList());

        // 根据command task生成flow
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                log.info("记录command开始执行时间。。。");
                // 更新command状态
                CommandEntity updateCommandEntity = commandRepository.findById(commandId).get();
                updateCommandEntity.setCommandState(CommandState.RUNNING);
                updateCommandEntity.setStartTime(new Date());
                commandRepository.saveAndFlush(updateCommandEntity);
            }
        });

        for (Runnable runnable : runnableList) {
            completableFuture = completableFuture.thenRunAsync(runnable);
        }


        completableFuture.whenComplete(new BiConsumer<Void, Throwable>() {
            @Override
            public void accept(Void unused, Throwable throwable) {
                if (throwable == null) {
                    log.info("记录command正常结束时间。。。");
                    // 更新command状态
                    CommandEntity updateCommandEntity = commandRepository.findById(commandId).get();
                    updateCommandEntity.setCommandState(CommandState.SUCCESS);
                    updateCommandEntity.setEndTime(new Date());
                    commandRepository.saveAndFlush(updateCommandEntity);
                }
            }
        });

        completableFuture.exceptionally(new Function<Throwable, Void>() {
            @Override
            public Void apply(Throwable throwable) {
                throwable.printStackTrace();
                log.info("调度程序发现异常：" + throwable.getMessage());
                log.info("记录command异常结束时间。。。");
                // 更新command状态
                CommandEntity updateCommandEntity = commandRepository.findById(commandId).get();
                updateCommandEntity.setCommandState(CommandState.ERROR);
                updateCommandEntity.setEndTime(new Date());
                commandRepository.saveAndFlush(updateCommandEntity);
                return null;
            }
        });
    }

}
