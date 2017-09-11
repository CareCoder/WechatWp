package com.yq.wechat.web.task;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.yq.wechat.itchar4j.context.Context;
import com.yq.wechat.itchar4j.core.Core;
import com.yq.wechat.itchar4j.utils.MyHttpClient;
import com.yq.wechat.itchar4j.utils.SleepUtils;
import com.yq.wechat.itchar4j.utils.tools.MessageTools;
import com.yq.wechat.web.base.RoomMsgTransform;
import com.yq.wechat.web.business.beans.CreateRoomBean;
import com.yq.wechat.web.business.beans.CreateRoomBean.RoomState;
import com.yq.wechat.web.constant.Constant;
import com.yq.wechat.web.context.WebContext;

/**
 * 自动创建房间任务线程
 * @author yq
 *
 */
@Component
@Scope("prototype")
public class CreateRoomTask implements Runnable{
	
	private Logger LOG = LoggerFactory.getLogger(CreateRoomTask.class);
	@Resource
	private Constant constant;
	
	@Override
	public void run() {
		while(true){
			try {
				Set<Entry<String, ConcurrentHashMap<String, CreateRoomBean>>> createRoomBeans = WebContext.INSTANCE.getCreateRoomBeans();
				for (Entry<String, ConcurrentHashMap<String, CreateRoomBean>> entry : createRoomBeans) {
					Set<Entry<String, CreateRoomBean>> entrySet = entry.getValue().entrySet();
					for (Entry<String, CreateRoomBean> createRoomBeanEntry : entrySet) {
						CreateRoomBean createRoomBean = createRoomBeanEntry.getValue();
						executeCreateRoom(createRoomBean);
					}
				}
				SleepUtils.sleep(constant.getCreateRoomInterval() * 1000);
			} catch (Exception e) {
				LOG.error("CreateRoomTask出现异常",e);
			}
		}
	}

	private void executeCreateRoom(CreateRoomBean createRoomBean) {
		LOG.debug("executeCreateRoom == " + createRoomBean.toString());
		RoomState roomState = createRoomBean.getRoomState();
		if(roomState == RoomState.ROOM_INIT){
			//应该去创建房间
			createRoom(createRoomBean);
		}else if(roomState == RoomState.ROOM_READY){
			//查询房间座位号
			queryRoomSeat(createRoomBean);
		}else if(roomState == RoomState.ROOM_STARTED){
			//开始之后把状态置为等待结束
			createRoomBean.setRoomState(RoomState.WAIT_END);
		}else if(roomState == RoomState.WAIT_END){
			//轮询什么时候牌局借宿了
			queryRoomSeat(createRoomBean);
		}else if(roomState == RoomState.ROOM_END){
			//重新改变房间的状态, 让任务线程执行创建房间
			createRoomBean.setRoomState(RoomState.ROOM_INIT);
		}
		if(createRoomBean.getCurNum() > 0 || roomState == RoomState.ROOM_INIT){
			sendMsg2WeChat(createRoomBean, roomState);
		}
	}
	
	/**
	 * 给群发送消息
	 * @param createRoomBean
	 */
	private void sendMsg2WeChat(CreateRoomBean createRoomBean, RoomState roomState) {
		String userName = createRoomBean.getUserName();
		String coreUuid = WebContext.INSTANCE.getCoreUuid(userName);
		Core core = Context.INSTANCE.getCore(coreUuid);
		if(core == null || ! core.isAlive()){
			//当用户已经退出微信登录之后那么后续的消息就不在发送,并且删除对象
			WebContext.INSTANCE.removeCreateRoomBean(userName);
			WebContext.INSTANCE.removeUser2Core(userName);
			LOG.warn("用户退出微信,删除所有任务,用户==username==" + userName);
		}
		MessageTools.sendMsgById(RoomMsgTransform.sendRoomMsg(createRoomBean, roomState), createRoomBean.getGroupId(), core);
	}

	/**
	 * 查询房间结算信息
	 * @param createRoomBean
	 */
	private void queryRoomSettlement(CreateRoomBean crb) {
		try {
			String url = String.format(constant.getQueryRoomSettlementUrl(), crb.getRoomId());
			HttpEntity entity = MyHttpClient.getInstance().doGet(url, null, true, null);
			String rsp = EntityUtils.toString(entity, Charset.defaultCharset());
			JSONObject rspJsonObj = JSONObject.parseObject(rsp);
			if(rspJsonObj != null){
				String settleInfo = rspJsonObj.getString("settleInfo");
				crb.setSettleInfo(settleInfo);
				//改变状态
				crb.setRoomState(RoomState.ROOM_END);
			}
		} catch (Exception e) {
			LOG.error("queryRoomSettlement ex", e);
		}
	}

	/**
	 * 查询房间座位号
	 * @param createRoomBean
	 */
	private void queryRoomSeat(CreateRoomBean crb) {
		try {
			String url = String.format(constant.getQueryRoomSeatUrl(),
					crb.getRoomId());
			HttpEntity entity = MyHttpClient.getInstance().doGet(url, null,
					true, null);
			String rsp = EntityUtils.toString(entity, Charset.defaultCharset());
			JSONObject rspJsonObj = JSONObject.parseObject(rsp);
			if (rspJsonObj != null) {
				int curNum = Integer.parseInt(rspJsonObj.getString("curNum"));
				int state = Integer.parseInt(rspJsonObj.getString("state"));
				// 改变状态
				if (state == 0) {
					// 未开始,暂时不处理
					crb.setCurNum(curNum);
					crb.setState(state);
				} else if (state == 1) {
					// 正在进行中
					crb.setCurNum(curNum);
					crb.setState(state);
					if(crb.getRoomState() == RoomState.ROOM_READY){
						//准备之后才能才是
						crb.setRoomState(RoomState.ROOM_STARTED);
					}
				} else if (state == 2) {
					// 结束了
					//查询房间结算信息
					queryRoomSettlement(crb);
					crb.setRoomState(RoomState.ROOM_END);
				}
			}
		} catch (Exception e) {
			LOG.error("queryRoomSeat ex", e);
		}
	}
	
	/**
	 * 应该去创建房间
	 * @param crb
	 */
	private void createRoom(CreateRoomBean crb) {
		try {
			String url = String.format(constant.getCreatrRoomUrl(), crb.getAccid(), crb.getOption(), crb.getMjType(), crb.getRoomType()); 
			HttpEntity entity = MyHttpClient.getInstance().doGet(url, null, true, null);
			String rsp = EntityUtils.toString(entity, Charset.defaultCharset());
			JSONObject rspJsonObj = JSONObject.parseObject(rsp);
			if(rspJsonObj != null){
				String roomCode = rspJsonObj.getString("roomCode");
				String roomId = rspJsonObj.getString("roomId");
				String status = rspJsonObj.getString("status");
				crb.setRoomCode(roomCode);
				crb.setRoomId(roomId);
				//创建房间成功,改变状态
				if(status.equals("-2")){
					crb.setRoomState(RoomState.ROOM_READY);
				}
			}
		} catch (ParseException | IOException e) {
			LOG.error("createRoom ex", e);
		}
	}

}
