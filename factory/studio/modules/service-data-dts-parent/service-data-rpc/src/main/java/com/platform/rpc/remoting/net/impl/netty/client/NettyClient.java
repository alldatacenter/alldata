package com.platform.rpc.remoting.net.impl.netty.client;

import com.platform.rpc.remoting.net.Client;
import com.platform.rpc.remoting.net.common.ConnectClient;
import com.platform.rpc.remoting.net.params.XxlRpcRequest;


public class NettyClient extends Client {

	private Class<? extends ConnectClient> connectClientImpl = NettyConnectClient.class;

	@Override
	public void asyncSend(String address, XxlRpcRequest xxlRpcRequest) throws Exception {
		ConnectClient.asyncSend(xxlRpcRequest, address, connectClientImpl, xxlRpcReferenceBean);
	}

}
