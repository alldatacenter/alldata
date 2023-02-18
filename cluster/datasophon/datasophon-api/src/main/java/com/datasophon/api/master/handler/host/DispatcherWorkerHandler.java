package com.datasophon.api.master.handler.host;

import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;

import java.net.UnknownHostException;

public interface DispatcherWorkerHandler {
    boolean handle(ClientSession session, HostInfo hostInfo) throws UnknownHostException;
}
