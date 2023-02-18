package com.datasophon.common.model;

import com.datasophon.common.enums.ReplyType;
import lombok.Data;

import java.io.Serializable;
@Data
public class AkkaRemoteReply implements Serializable {

    private ReplyType replyType;

    private String hostname;

    private String serviceRoleName;

    private String hostCommandId;

    private String msg;
}
