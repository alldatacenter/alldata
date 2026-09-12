
package com.platform.modules.mnt.websocket;

import lombok.Data;

/**
 * @author AllDataDC
 * @date 2023-01-27 9:55
 */
@Data
public class SocketMsg {
	private String msg;
	private MsgType msgType;

	public SocketMsg(String msg, MsgType msgType) {
		this.msg = msg;
		this.msgType = msgType;
	}
}
