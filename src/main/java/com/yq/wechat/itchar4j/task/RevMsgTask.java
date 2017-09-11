package com.yq.wechat.itchar4j.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yq.wechat.itchar4j.beans.BaseMsg;
import com.yq.wechat.itchar4j.context.Context;
import com.yq.wechat.itchar4j.core.Core;
import com.yq.wechat.itchar4j.core.MsgCenter;
import com.yq.wechat.itchar4j.utils.MyHttpClient;
import com.yq.wechat.itchar4j.utils.SleepUtils;
import com.yq.wechat.itchar4j.utils.enums.RetCodeEnum;
import com.yq.wechat.itchar4j.utils.enums.StorageLoginInfoEnum;
import com.yq.wechat.itchar4j.utils.enums.URLEnum;
import com.yq.wechat.itchar4j.utils.enums.parameters.BaseParaEnum;
import com.yq.wechat.itchar4j.utils.tools.CommonTools;

/**
 * 生产消息
 * @author yq
 *
 */
public class RevMsgTask implements Runnable{
	private static Logger LOG = LoggerFactory.getLogger(RevMsgTask.class);
	
	@Override
	public void run() {
		while(true){
			Set<Entry<String, Core>> cores = Context.INSTANCE.getCores();
			for (Entry<String, Core> coreEntry : cores) {
				Core core = null;
				try {
					core = coreEntry.getValue();
					if(core == null){
						continue;
					}
					if (System.currentTimeMillis() - core.getLastNormalRetcodeTime() > 60 * 1000) { // 超过60秒，判为离线
						core.setAlive(false);
						LOG.info("微信已离线===" + core.getUuid());
						continue;
					}
					Map<String, String> resultMap = syncCheck(core);
					if(resultMap == null || resultMap.isEmpty()){
						continue;
					}
					LOG.debug(JSONObject.toJSONString(resultMap));
					if(resultMap == null || resultMap.isEmpty()){
						continue;
					}
					String retcode = resultMap.get("retcode");
					String selector = resultMap.get("selector");
					if (retcode.equals(RetCodeEnum.UNKOWN.getCode())) {
						LOG.debug(RetCodeEnum.UNKOWN.getType());
						continue;
					} else if (retcode.equals(RetCodeEnum.LOGIN_OUT.getCode())) { // 退出
						LOG.debug(RetCodeEnum.LOGIN_OUT.getType());
						break;
					} else if (retcode.equals(RetCodeEnum.LOGIN_OTHERWHERE.getCode())) { // 其它地方登陆
						LOG.debug(RetCodeEnum.LOGIN_OTHERWHERE.getType());
						break;
					} else if (retcode.equals(RetCodeEnum.MOBILE_LOGIN_OUT.getCode())) { // 移动端退出
						LOG.debug(RetCodeEnum.MOBILE_LOGIN_OUT.getType());
						break;
					} else if (retcode.equals(RetCodeEnum.NORMAL.getCode())) {
						core.setLastNormalRetcodeTime(System.currentTimeMillis()); // 最后收到正常报文时间
						JSONObject msgObj = webWxSync(core);
						if (selector.equals("2")) {
							if (msgObj != null) {
								try {
									JSONArray msgList = new JSONArray();
									msgList = msgObj.getJSONArray("AddMsgList");
									msgList = MsgCenter.produceMsg(msgList, core);
									for (int j = 0; j < msgList.size(); j++) {
										BaseMsg baseMsg = JSON.parseObject(msgList.getJSONObject(j).toJSONString(), BaseMsg.class);
										//记录一下产生这个消息的uuid,之后处理basemsg的时候可以通过basemsg超找到所有信息
										baseMsg.setUuid(core.getUuid());
//										core.getMsgList().add(baseMsg);
										//这里交给一个task来执行
										//重要 这里是阻塞线程,所有如果有太多消息没被处理的话 就会出现 卡顿显像,所以需要多配置几个线程来处理
										Context.INSTANCE.putBaseMsg(baseMsg);
									}
								} catch (Exception e) {
									LOG.debug(e.getMessage());
								}
							}
						} else if (selector.equals("7")) {
							webWxSync(core);
						} else if (selector.equals("4")) {
							continue;
						} else if (selector.equals("3")) {
							continue;
						} else if (selector.equals("6")) {
							if (msgObj != null) {
								try {
									JSONArray msgList = new JSONArray();
									msgList = msgObj.getJSONArray("AddMsgList");
									JSONArray modContactList = msgObj.getJSONArray("ModContactList"); // 存在删除或者新增的好友信息
									msgList = MsgCenter.produceMsg(msgList, core);
									for (int j = 0; j < msgList.size(); j++) {
										JSONObject userInfo = modContactList.getJSONObject(j);
										// 存在主动加好友之后的同步联系人到本地
										core.getContactList().add(userInfo);
									}
								} catch (Exception e) {
									LOG.debug(e.getMessage());
								}
							}
						}
					} else {
						LOG.debug("接收消息的时候收到未捕获回复==uuid==" + core.getUuid()+"==nickName"+core.getNickName());
					}
				} catch (Exception e) {
					LOG.error(e.getMessage());
				}
				SleepUtils.sleep(50);
			}
		}
	}
	
	private Map<String, String> syncCheck(Core core) {
		if(core == null){
			return new HashMap<String, String>();
		}
		if(!core.isAlive()){
			//还处于未登录状态
			return new HashMap<String, String>();
		}
		Map<String, String> resultMap = new HashMap<String, String>();
		// 组装请求URL和参数
		String url = core.getLoginInfo().get(StorageLoginInfoEnum.syncUrl.getKey()) + URLEnum.SYNC_CHECK_URL.getUrl();
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		for (BaseParaEnum baseRequest : BaseParaEnum.values()) {
			params.add(new BasicNameValuePair(baseRequest.para().toLowerCase(),
					core.getLoginInfo().get(baseRequest.value()).toString()));
		}
		params.add(new BasicNameValuePair("r", String.valueOf(new Date().getTime())));
		params.add(new BasicNameValuePair("synckey", (String) core.getLoginInfo().get("synckey")));
		params.add(new BasicNameValuePair("_", String.valueOf(new Date().getTime())));
		SleepUtils.sleep(7);
		try {
			HttpEntity entity = MyHttpClient.getInstance().doGet(url, params, true, null);
			if (entity == null) {
				resultMap.put("retcode", "9999");
				resultMap.put("selector", "9999");
				return resultMap;
			}
			String text = EntityUtils.toString(entity);
			String regEx = "window.synccheck=\\{retcode:\"(\\d+)\",selector:\"(\\d+)\"\\}";
			Matcher matcher = CommonTools.getMatcher(regEx, text);
			if (!matcher.find() || matcher.group(1).equals("2")) {
				LOG.debug(String.format("Unexpected sync check result: %s", text));
			} else {
				resultMap.put("retcode", matcher.group(1));
				resultMap.put("selector", matcher.group(2));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultMap;
	}
	
	
	/**
	 * 同步消息 sync the messages
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月12日 上午12:24:55
	 * @return
	 */
	private JSONObject webWxSync(Core core) {
		JSONObject result = null;
		String url = String.format(URLEnum.WEB_WX_SYNC_URL.getUrl(),
				core.getLoginInfo().get(StorageLoginInfoEnum.url.getKey()),
				core.getLoginInfo().get(StorageLoginInfoEnum.wxsid.getKey()),
				core.getLoginInfo().get(StorageLoginInfoEnum.skey.getKey()),
				core.getLoginInfo().get(StorageLoginInfoEnum.pass_ticket.getKey()));
		Map<String, Object> paramMap = core.getParamMap();
		paramMap.put(StorageLoginInfoEnum.SyncKey.getKey(),
				core.getLoginInfo().get(StorageLoginInfoEnum.SyncKey.getKey()));
		paramMap.put("rr", -new Date().getTime() / 1000);
		String paramStr = JSON.toJSONString(paramMap);
		try {
			HttpEntity entity = MyHttpClient.getInstance().doPost(url, paramStr);
			String text = EntityUtils.toString(entity, Consts.UTF_8);
			JSONObject obj = JSON.parseObject(text);
			if (obj.getJSONObject("BaseResponse").getInteger("Ret") != 0) {
				result = null;
			} else {
				result = obj;
				core.getLoginInfo().put(StorageLoginInfoEnum.SyncKey.getKey(), obj.getJSONObject("SyncCheckKey"));
				JSONArray syncArray = obj.getJSONObject(StorageLoginInfoEnum.SyncKey.getKey()).getJSONArray("List");
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < syncArray.size(); i++) {
					sb.append(syncArray.getJSONObject(i).getString("Key") + "_"
							+ syncArray.getJSONObject(i).getString("Val") + "|");
				}
				String synckey = sb.toString();
				core.getLoginInfo().put(StorageLoginInfoEnum.synckey.getKey(),
						synckey.substring(0, synckey.length() - 1));// 1_656161336|2_656161626|3_656161313|11_656159955|13_656120033|201_1492273724|1000_1492265953|1001_1492250432|1004_1491805192
			}
		} catch (Exception e) {
			LOG.debug(e.getMessage());
		}
		return result;

	}
}
