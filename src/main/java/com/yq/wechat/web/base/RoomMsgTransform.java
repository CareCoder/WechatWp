package com.yq.wechat.web.base;

import com.yq.wechat.web.business.beans.CreateRoomBean;
import com.yq.wechat.web.business.beans.CreateRoomBean.RoomState;


/**
 * 发送到微信端,消息转换
 * @author yq
 *
 */
public class RoomMsgTransform {

	/**
	 * 创建房间成功后,转换消息
	 * @param createRoomBean
	 * @return
	 */
	public static String sendRoomMsg(CreateRoomBean createRoomBean, RoomState roomState) {
		String res = null;
		if(roomState == RoomState.ROOM_INIT){
			res = createRoomMsg(createRoomBean);
		}else if(roomState == RoomState.ROOM_READY){
			res = queryRoomSeatRoomMsg(createRoomBean);
		}else if(roomState == RoomState.ROOM_STARTED){
			res = startRoomMsg(createRoomBean);
		}else if(roomState == RoomState.ROOM_END){
			res = queryRoomSettlementRoomMsg(createRoomBean);
		}
		return res;
	}

	private static String startRoomMsg(CreateRoomBean crb) {
		StringBuilder sb = new StringBuilder();
		sb.append("房间号[ ");
		sb.append(crb.getRoomCode());
		sb.append(" ]");
		sb.append(" 开始啦!!!");
		return sb.toString();
	}

	private static String createRoomMsg(CreateRoomBean crb) {
		StringBuilder sb = new StringBuilder();
		sb.append("我创建了一个房间, 房间号是[ ");
		sb.append(crb.getRoomCode());
		sb.append(" ]");
		return sb.toString();
	}

	private static String queryRoomSeatRoomMsg(CreateRoomBean createRoomBean) {
		StringBuilder sb = new StringBuilder();
		sb.append("房间[");
		sb.append(createRoomBean.getRoomCode());
		sb.append("]");
		sb.append("房间还剩[ ");
		sb.append(createRoomBean.getCurNum());
		sb.append(" ]座位");
		return sb.toString();
	}

	private static String queryRoomSettlementRoomMsg(CreateRoomBean createRoomBean) {
		StringBuilder sb = new StringBuilder();
		sb.append("房间[");
		sb.append(createRoomBean.getRoomCode());
		sb.append("]");
		sb.append(" 牌局结算: ");
		sb.append(createRoomBean.getSettleInfo());
		return sb.toString();
	}

}
