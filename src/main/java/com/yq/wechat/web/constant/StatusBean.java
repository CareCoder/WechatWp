package com.yq.wechat.web.constant;

public class StatusBean {
	private int statusCode;
	private String statusInfo;

	public StatusBean(int statusCode, String statusInfo) {
		super();
		this.statusCode = statusCode;
		this.statusInfo = statusInfo;
	}

	public StatusBean() {
		super();
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusInfo() {
		return statusInfo;
	}

	public void setStatusInfo(String statusInfo) {
		this.statusInfo = statusInfo;
	}

	@Override
	public String toString() {
		return "StatusBean [statusCode=" + statusCode + ", statusInfo="
				+ statusInfo + "]";
	}

}
