package com.datasophon.api.master.handler.host;

import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class DispatcherWorkerHandlerChain {

    private List<DispatcherWorkerHandler> handlers = new ArrayList<>();

    public void addHandler(DispatcherWorkerHandler handler) {
        this.handlers.add(handler);
    }

    public void handle(ClientSession session, HostInfo hostInfo) throws UnknownHostException {
        for (DispatcherWorkerHandler handler : handlers) {
            boolean handled = handler.handle(session, hostInfo);
            if (!handled) {
                break;
            }
        }
    }
}
