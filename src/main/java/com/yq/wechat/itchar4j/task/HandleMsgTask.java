package com.yq.wechat.itchar4j.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yq.wechat.itchar4j.beans.BaseMsg;
import com.yq.wechat.itchar4j.context.Context;
import com.yq.wechat.itchar4j.core.Core;
import com.yq.wechat.itchar4j.core.MsgCenter;
import com.yq.wechat.itchar4j.msgHandler.NonMsgHandler;

/**
 * 处理消息
 * @author yq
 *
 */
public class HandleMsgTask implements Runnable{
	private Logger LOG = LoggerFactory.getLogger(HandleMsgTask.class);
	
	@Override
	public void run() {
		while(true){
			BaseMsg takeBaseMsg = null;
			try {
				takeBaseMsg = Context.INSTANCE.takeBaseMsg();
				Core core = Context.INSTANCE.getCore(takeBaseMsg.getUuid());
				MsgCenter.handleMsg(new NonMsgHandler(), takeBaseMsg, core);
			} catch (Exception e) {
				//这里出现异常暂时先把消息 抛出,因为重新加入队列可能再次运行的时候还是会出现异常
				LOG.error("HandleMsgTask处理消息出现异常===="+takeBaseMsg.toString());
			}
		}
	}

}
