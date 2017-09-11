package com.yq.wechat.itchar4j.msgHandler;

import org.apache.log4j.Logger;

import com.yq.wechat.itchar4j.beans.BaseMsg;


/**
 * 对于任何消息都不处理
 * @author yq
 *
 */
public class NonMsgHandler implements IMsgHandlerFace{
	Logger LOG = Logger.getLogger(NonMsgHandler.class);

	@Override
	public String textMsgHandle(BaseMsg msg) {
		return "";
	}

	@Override
	public String picMsgHandle(BaseMsg msg) {
		return "";
	}

	@Override
	public String voiceMsgHandle(BaseMsg msg) {
		return "";
	}

	@Override
	public String viedoMsgHandle(BaseMsg msg) {
		return "";
	}

	@Override
	public String nameCardMsgHandle(BaseMsg msg) {
		return "";
	}

	@Override
	public void sysMsgHandle(BaseMsg msg) {
		
	}

	@Override
	public String verifyAddFriendMsgHandle(BaseMsg msg) {
		return "";
	}

	@Override
	public String mediaMsgHandle(BaseMsg msg) {
		return "";
	}
}
