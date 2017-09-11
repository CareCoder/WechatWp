package com.yq.wechat.web.business.service;

import com.alibaba.fastjson.JSONObject;
import com.yq.wechat.web.business.beans.CreateRoomBean;

public interface CoreService {
	/**用户登录*/
	public String login(String userName);
	/**用户登出*/
	public int logout(String userName);
	/**获取uuid*/
	public String getUuid();
	/**获取登录二维码*/
	public String getQR(String userName);
	/**查询指定uuid的用户是否扫码登录成功了*/
	public String getLoginInfo(String userName);
	/**获取玩家自动创建房间的信息*/
	public String getCreateRoomInfo(String userName);
	/**根据指定uuid查询用户信息*/
	public JSONObject getUserInfo(String userName);
	/**需要判断 username 和core是否存在*/
	public boolean validateCreateRoom(CreateRoomBean createRoomBean);
	/**取消自动创建房间任务*/
	public int cancelCreateRoom(String userName, String groupId);
}
