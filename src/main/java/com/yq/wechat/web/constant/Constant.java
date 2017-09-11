package com.yq.wechat.web.constant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信机器人的常用变量
 * @author yq
 *
 */
@Component
@ConfigurationProperties("constant")
public class Constant {
	//处理消息的线程数
	private int handleMsgTaskThreadNum = 3;
	//自动创建房间检测间隔(单位:秒)
	private  int createRoomInterval = 30;
	//连接游戏服的地址
	private String activityServerAddr = "http://192.169.1.31:10513";
	//创建房间的url
	private String creatrRoomUrl = activityServerAddr +"/data?cmd=500&mAccId=%s&option=%s&mjType=%s&roomType=%s";
	//查询房间还有多少空位
	private String queryRoomSeatUrl = activityServerAddr +"/data?cmd=501&roomId=%s";
	//查询房间结算信息
	private String queryRoomSettlementUrl = activityServerAddr +"/data?cmd=502&roomId=%s";
	
	public int getHandleMsgTaskThreadNum() {
		return handleMsgTaskThreadNum;
	}
	public void setHandleMsgTaskThreadNum(int handleMsgTaskThreadNum) {
		this.handleMsgTaskThreadNum = handleMsgTaskThreadNum;
	}
	public int getCreateRoomInterval() {
		return createRoomInterval;
	}
	public void setCreateRoomInterval(int createRoomInterval) {
		this.createRoomInterval = createRoomInterval;
	}
	public String getActivityServerAddr() {
		return activityServerAddr;
	}
	public void setActivityServerAddr(String activityServerAddr) {
		this.activityServerAddr = activityServerAddr;
	}
	public String getCreatrRoomUrl() {
		return creatrRoomUrl;
	}
	public void setCreatrRoomUrl(String creatrRoomUrl) {
		this.creatrRoomUrl = creatrRoomUrl;
	}
	public String getQueryRoomSeatUrl() {
		return queryRoomSeatUrl;
	}
	public void setQueryRoomSeatUrl(String queryRoomSeatUrl) {
		this.queryRoomSeatUrl = queryRoomSeatUrl;
	}
	public String getQueryRoomSettlementUrl() {
		return queryRoomSettlementUrl;
	}
	public void setQueryRoomSettlementUrl(String queryRoomSettlementUrl) {
		this.queryRoomSettlementUrl = queryRoomSettlementUrl;
	}
}
