package com.datasophon.api.master.handler.service;

import akka.actor.ActorSystem;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import lombok.Data;

@Data
public abstract class ServiceHandler {

    private ServiceHandler next;


    public abstract ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo ) throws Exception;

}
