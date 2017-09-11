package com.yq.wechat.itchar4j.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yq.wechat.itchar4j.utils.MyHttpClient;
import com.yq.wechat.itchar4j.utils.enums.ResultEnum;
import com.yq.wechat.itchar4j.utils.enums.URLEnum;
import com.yq.wechat.itchar4j.utils.enums.parameters.UUIDParaEnum;
import com.yq.wechat.itchar4j.utils.tools.CommonTools;

public class LoginServiceImpl {
	private static Logger LOG = LoggerFactory.getLogger(LoginServiceImpl.class);
	
	/**
	 * 获取登录的uuid,uuid的作用是用来获取登录二维码的
	 * @return
	 */
	public static String getUuid() {
		// 组装参数和URL
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair(UUIDParaEnum.APP_ID.para(), UUIDParaEnum.APP_ID.value()));
		params.add(new BasicNameValuePair(UUIDParaEnum.FUN.para(), UUIDParaEnum.FUN.value()));
		params.add(new BasicNameValuePair(UUIDParaEnum.LANG.para(), UUIDParaEnum.LANG.value()));
		params.add(new BasicNameValuePair(UUIDParaEnum._.para(), String.valueOf(System.currentTimeMillis())));

		HttpEntity entity = MyHttpClient.getInstance().doGet(URLEnum.UUID_URL.getUrl(), params, true, null);

		try {
			String result = EntityUtils.toString(entity);
			String regEx = "window.QRLogin.code = (\\d+); window.QRLogin.uuid = \"(\\S+?)\";";
			Matcher matcher = CommonTools.getMatcher(regEx, result);
			if (matcher.find()) {
				if ((ResultEnum.SUCCESS.getCode().equals(matcher.group(1)))) {
					return matcher.group(2);
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return "";
	}
	
	/**
	 * 获取登录二维码的二进制数据
	 * @param uuid
	 * @return
	 */
	public static byte[] getQR(String uuid) {
		try {
			String qrUrl = getQRUrl(uuid);
			HttpEntity entity = MyHttpClient.getInstance().doGet(qrUrl, null, true, null);
			byte[] bytes = EntityUtils.toByteArray(entity);
			return bytes;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 获取登录二维码的二进制数据
	 * @param uuid
	 * @return
	 */
	public static String getQRUrl(String uuid) {
		return URLEnum.QRCODE_URL.getUrl() + uuid;
	}
}
