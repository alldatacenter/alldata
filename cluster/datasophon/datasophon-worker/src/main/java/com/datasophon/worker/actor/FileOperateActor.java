package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import cn.hutool.core.io.FileUtil;
import com.datasophon.common.command.FileOperateCommand;
import com.datasophon.common.utils.ExecResult;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.TreeSet;

public class FileOperateActor extends UntypedActor {
    @Override
    public void onReceive(Object msg) throws Throwable, Throwable {
        if (msg instanceof FileOperateCommand) {
            ExecResult execResult = new ExecResult();
            FileOperateCommand fileOperateCommand = (FileOperateCommand) msg;
            TreeSet<String> lines = fileOperateCommand.getLines();
            if (Objects.nonNull(lines) && lines.size() > 0) {
                File file = FileUtil.writeLines(lines, fileOperateCommand.getPath(), Charset.defaultCharset());
                if(file.exists()){
                    execResult.setExecResult(true);
                }
            }else {
                FileUtil.writeUtf8String(fileOperateCommand.getContent(),fileOperateCommand.getPath());
            }
            getSender().tell(execResult,getSelf());
        } else {
            unhandled(msg);
        }
    }
}
