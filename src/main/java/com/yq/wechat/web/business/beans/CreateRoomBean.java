package com.yq.wechat.web.business.beans;

public class CreateRoomBean {
	private String userName;
	private String groupId;
	private int mjType;
	private String option;
	private long accid;
	private int roomType;
	//上面6个是参数
	private String roomCode;
	private String roomId;
	private int curNum;//当前人数
	private int state;//服务器返回的状态码
	private String settleInfo;//json格式的结算信息
	private RoomState roomState = RoomState.ROOM_INIT;
	
	public enum RoomState{
		ROOM_INIT,//当前应该创建房间
		ROOM_READY,//准备状态
		ROOM_STARTED,//房间开始了
		WAIT_END,//等待结束
		ROOM_END;//当前应该查询房间的结算
	};

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public int getMjType() {
		return mjType;
	}

	public void setMjType(int mjType) {
		this.mjType = mjType;
	}

	public String getOption() {
		return option;
	}

	public void setOption(String option) {
		this.option = option;
	}

	public long getAccid() {
		return accid;
	}

	public void setAccid(long accid) {
		this.accid = accid;
	}

	public RoomState getRoomState() {
		return roomState;
	}

	public void setRoomState(RoomState roomState) {
		this.roomState = roomState;
	}

	public int getRoomType() {
		return roomType;
	}

	public void setRoomType(int roomType) {
		this.roomType = roomType;
	}

	public String getRoomCode() {
		return roomCode;
	}

	public void setRoomCode(String roomCode) {
		this.roomCode = roomCode;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public int getCurNum() {
		return curNum;
	}

	public void setCurNum(int curNum) {
		this.curNum = curNum;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getSettleInfo() {
		return settleInfo;
	}

	public void setSettleInfo(String settleInfo) {
		this.settleInfo = settleInfo;
	}

	@Override
	public String toString() {
		return "CreateRoomBean [userName=" + userName + ", groupId=" + groupId
				+ ", mjType=" + mjType + ", option=" + option + ", accid="
				+ accid + ", roomType=" + roomType + ", roomCode=" + roomCode
				+ ", roomId=" + roomId + ", curNum=" + curNum + ", state="
				+ state + ", roomState=" + roomState + "]";
	}
	
}
