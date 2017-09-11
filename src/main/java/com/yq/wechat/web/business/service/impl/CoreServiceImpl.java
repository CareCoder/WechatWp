package com.yq.wechat.web.business.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yq.wechat.itchar4j.context.Context;
import com.yq.wechat.itchar4j.core.Core;
import com.yq.wechat.itchar4j.service.LoginServiceImpl;
import com.yq.wechat.itchar4j.utils.MyHttpClient;
import com.yq.wechat.itchar4j.utils.enums.LoginParaEnum;
import com.yq.wechat.itchar4j.utils.enums.ResultEnum;
import com.yq.wechat.itchar4j.utils.enums.StorageLoginInfoEnum;
import com.yq.wechat.itchar4j.utils.enums.URLEnum;
import com.yq.wechat.itchar4j.utils.enums.parameters.StatusNotifyParaEnum;
import com.yq.wechat.itchar4j.utils.tools.CommonTools;
import com.yq.wechat.web.business.beans.CreateRoomBean;
import com.yq.wechat.web.business.service.CoreService;
import com.yq.wechat.web.constant.StatusBean;
import com.yq.wechat.web.constant.StatusCode;
import com.yq.wechat.web.context.WebContext;

@Service
public class CoreServiceImpl implements CoreService{
	private static Logger LOG = LoggerFactory.getLogger(CoreServiceImpl.class);

	@Override
	public String login(String userName) {
		String coreUuid = WebContext.INSTANCE.getCoreUuid(userName);
		boolean needLogin = false;
		if(coreUuid == null || coreUuid.length() < 0){
			//玩家未登录过
			needLogin = true;
		}else{
			Core core = Context.INSTANCE.getCore(coreUuid);
			if(core == null || ! core.isAlive()){
				//core缓存已经失效　或者缓存已经未存活了
				needLogin = true;
			}
		}
		StatusBean sb = null;
		if(needLogin){
			//需要登录
			coreUuid = getUuid();
			WebContext.INSTANCE.addUser2Core(userName, coreUuid);
			//生成一个新的core,以前的代码被删除了
			Core core = new Core();
			core.setUuid(coreUuid);
			Context.INSTANCE.addCore(core);
			sb = new StatusBean(StatusCode.NEED_LOGIN, "need login");
		}else{
			sb = new StatusBean(StatusCode.SUCCESS, "already login");
		}
		JSONObject jb = (JSONObject) JSON.toJSON(sb);
		jb.put("uuid", coreUuid);
		return jb.toJSONString();
	}
	
	@Override
	public int logout(String userName){
		try {
			String uuid = WebContext.INSTANCE.removeUser2Core(userName);
			WebContext.INSTANCE.removeCreateRoomBean(userName);
			Context.INSTANCE.removeCore(uuid);
		} catch (Exception e) {
			LOG.error("logout ex",e);
			return StatusCode.ERROR;
		}
		return StatusCode.SUCCESS;
	}
	
	@Override
	public String getUuid() {
		return LoginServiceImpl.getUuid();
	}
	
	@Override
	public String getQR(String userName) {
		String uuid = WebContext.INSTANCE.getCoreUuid(userName);
		if(uuid == null || uuid.length() <= 0){
			return "";
		}
		JSONObject jb = new JSONObject();
		String qrUrl = LoginServiceImpl.getQRUrl(uuid);
		jb.put("qrUrl", qrUrl);
		return jb.toJSONString();
	}

	@Override
	public String getLoginInfo(String userName) {
		String uuid = WebContext.INSTANCE.getCoreUuid(userName);
		if(uuid == null){
			return ResultEnum.NOT_FOND_UUID.getCode();
		}
		Core core = Context.INSTANCE.getCore(uuid);
		
		if(core == null){
			return ResultEnum.NOT_FOND_UUID.getCode();
		}
		
		//玩家已经登录直接返回信息
		if(core.isAlive()){
			return ResultEnum.SUCCESS.getCode();
		}
		
		// 组装参数和URL
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair(LoginParaEnum.LOGIN_ICON.para(), LoginParaEnum.LOGIN_ICON.value()));
		params.add(new BasicNameValuePair(LoginParaEnum.UUID.para(), core.getUuid()));
		params.add(new BasicNameValuePair(LoginParaEnum.TIP.para(), LoginParaEnum.TIP.value()));
		
		long millis = System.currentTimeMillis();
		params.add(new BasicNameValuePair(LoginParaEnum.R.para(), String.valueOf(millis / 1579L)));
		params.add(new BasicNameValuePair(LoginParaEnum._.para(), String.valueOf(millis)));
		HttpEntity entity = MyHttpClient.getInstance().doGet(URLEnum.LOGIN_URL.getUrl(), params, true, null);
		String status = "";
		try {
			String result = EntityUtils.toString(entity);
			status = checklogin(result);

			if (ResultEnum.SUCCESS.getCode().equals(status)) {
				processLoginInfo(result, core); // 处理结果
				core.setAlive(true);
				boolean webWxinitRes = webWxInit(core);//初始化core信息
				if(!webWxinitRes){
					//初始化core信息失败
					return ResultEnum.INIT_CORE_FAIL.getCode();
				}
				//开启微信状态通知
				wxStatusNotify(core);
			}
		} catch (Exception e) {
			LOG.error("微信登陆异常！", e);
		}
		return status;
	}
	
	@Override
	public String getCreateRoomInfo(String userName){
		ConcurrentHashMap<String, CreateRoomBean> createRoomBeans = WebContext.INSTANCE.getCreateRoomBeans(userName);
		Collection<CreateRoomBean> values = createRoomBeans.values();
		return JSON.toJSONString(values);
	}
	
	@Override
	public JSONObject getUserInfo(String userName) {
		String uuid = WebContext.INSTANCE.getCoreUuid(userName);
		if(uuid == null){
			return null;
		}
		Core core = Context.INSTANCE.getCore(uuid);
		if(core == null){
			return null;
		}
		JSONObject userSelf = core.getUserSelf();//获取用户自己的信息
//		List<String> groupIdList = core.getGroupIdList();//获取所有群的id
		List<JSONObject> groupList = core.getGroupList();//获取所有的群信息
		JSONObject resJsonObject = new JSONObject();
		resJsonObject.put("userSelf", userSelf);
		resJsonObject.put("groupList", groupList);
		return resJsonObject;
	}
	
	@Override
	public boolean validateCreateRoom(CreateRoomBean crb){
		try {
			if(crb == null){
				return false;
			}
			String userName = crb.getUserName();
			if(userName == null){
				return false;
			}
			String coreUuid = WebContext.INSTANCE.getCoreUuid(userName);
			if(coreUuid == null){
				return false;
			}
			Core core = Context.INSTANCE.getCore(coreUuid);
			if(core == null || ! core.isAlive()){
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public int cancelCreateRoom(String userName, String groupId){
		try {
			if(groupId.equals("@@@@")){
				//删除这个玩家的所有任务
				WebContext.INSTANCE.removeCreateRoomBean(userName);
			}else{
				CreateRoomBean createRoomBean = WebContext.INSTANCE.removeCreateRoomBean(userName, groupId);
				if(createRoomBean != null){
					return StatusCode.SUCCESS;
				}else{
					return StatusCode.NOT_FOUND;
				}
			}
		} catch (Exception e) {
			LOG.error("cancelCreateRoom",e);
		}
		return StatusCode.ERROR;
	}
	
	
	private void wxStatusNotify(Core core) {
		// 组装请求URL和参数
		String url = String.format(URLEnum.STATUS_NOTIFY_URL.getUrl(),
				core.getLoginInfo().get(StorageLoginInfoEnum.pass_ticket.getKey()));

		Map<String, Object> paramMap = core.getParamMap();
		paramMap.put(StatusNotifyParaEnum.CODE.para(), StatusNotifyParaEnum.CODE.value());
		paramMap.put(StatusNotifyParaEnum.FROM_USERNAME.para(), core.getUserName());
		paramMap.put(StatusNotifyParaEnum.TO_USERNAME.para(), core.getUserName());
		paramMap.put(StatusNotifyParaEnum.CLIENT_MSG_ID.para(), System.currentTimeMillis());
		String paramStr = JSON.toJSONString(paramMap);

		try {
			HttpEntity entity = MyHttpClient.getInstance().doPost(url, paramStr);
			EntityUtils.toString(entity, Consts.UTF_8);
		} catch (Exception e) {
			LOG.error("微信状态通知接口失败！", e);
		}

	
	}

	private boolean webWxInit(Core core) {
		core.setLastNormalRetcodeTime(System.currentTimeMillis());
		// 组装请求URL和参数
		String url = String.format(URLEnum.INIT_URL.getUrl(),
				core.getLoginInfo().get(StorageLoginInfoEnum.url.getKey()),
				String.valueOf(System.currentTimeMillis() / 3158L),
				core.getLoginInfo().get(StorageLoginInfoEnum.pass_ticket.getKey()));

		Map<String, Object> paramMap = core.getParamMap();

		// 请求初始化接口
		HttpEntity entity = MyHttpClient.getInstance().doPost(url, JSON.toJSONString(paramMap));
		try {
			String result = EntityUtils.toString(entity, Consts.UTF_8);
			System.out.println("==="+result+"+++");
			JSONObject obj = JSON.parseObject(result);

			JSONObject user = obj.getJSONObject(StorageLoginInfoEnum.User.getKey());
			JSONObject syncKey = obj.getJSONObject(StorageLoginInfoEnum.SyncKey.getKey());

			core.getLoginInfo().put(StorageLoginInfoEnum.InviteStartCount.getKey(),
					obj.getInteger(StorageLoginInfoEnum.InviteStartCount.getKey()));
			core.getLoginInfo().put(StorageLoginInfoEnum.SyncKey.getKey(), syncKey);

			JSONArray syncArray = syncKey.getJSONArray("List");
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < syncArray.size(); i++) {
				sb.append(syncArray.getJSONObject(i).getString("Key") + "_"
						+ syncArray.getJSONObject(i).getString("Val") + "|");
			}
			// 1_661706053|2_661706420|3_661706415|1000_1494151022|
			String synckey = sb.toString();

			// 1_661706053|2_661706420|3_661706415|1000_1494151022
			core.getLoginInfo().put(StorageLoginInfoEnum.synckey.getKey(), synckey.substring(0, synckey.length() - 1));// 1_656161336|2_656161626|3_656161313|11_656159955|13_656120033|201_1492273724|1000_1492265953|1001_1492250432|1004_1491805192
			core.setUserName(user.getString("UserName"));
			core.setNickName(user.getString("NickName"));
			core.setUserSelf(obj.getJSONObject("User"));
			
			String chatSet = obj.getString("ChatSet");
			String[] chatSetArray = chatSet.split(",");
			for (int i = 0; i < chatSetArray.length; i++) {
				if (chatSetArray[i].indexOf("@@") != -1) {
					// 更新GroupIdList
					core.getGroupIdList().add(chatSetArray[i]); //
				}
			}
			
			JSONArray contactListArray = obj.getJSONArray("ContactList");
			for (int i = 0; i < contactListArray.size(); i++) {
				JSONObject o = contactListArray.getJSONObject(i);
				if (o.getString("UserName").indexOf("@@") != -1) {
					core.getGroupIdList().add(o.getString("UserName")); //
					// 更新GroupIdList
					core.getGroupList().add(o); // 更新GroupList
					core.getGroupNickNameList().add(o.getString("NickName"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 检查登陆状态
	 *
	 * @param result
	 * @return
	 */
	public String checklogin(String result) {
		String regEx = "window.code=(\\d+)";
		Matcher matcher = CommonTools.getMatcher(regEx, result);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
	
	/**
	 * 处理登陆信息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月9日 下午12:16:26
	 * @param result
	 */
	private void processLoginInfo(String loginContent, Core core) {
		String regEx = "window.redirect_uri=\"(\\S+)\";";
		Matcher matcher = CommonTools.getMatcher(regEx, loginContent);
		if (matcher.find()) {
			String originalUrl = matcher.group(1);
			String url = originalUrl.substring(0, originalUrl.lastIndexOf('/')); // https://wx2.qq.com/cgi-bin/mmwebwx-bin
			core.getLoginInfo().put("url", url);
			Map<String, List<String>> possibleUrlMap = this.getPossibleUrlMap();
			Iterator<Entry<String, List<String>>> iterator = possibleUrlMap.entrySet().iterator();
			Map.Entry<String, List<String>> entry;
			String fileUrl;
			String syncUrl;
			while (iterator.hasNext()) {
				entry = iterator.next();
				String indexUrl = entry.getKey();
				fileUrl = "https://" + entry.getValue().get(0) + "/cgi-bin/mmwebwx-bin";
				syncUrl = "https://" + entry.getValue().get(1) + "/cgi-bin/mmwebwx-bin";
				if (core.getLoginInfo().get("url").toString().contains(indexUrl)) {
					core.setIndexUrl(indexUrl);
					core.getLoginInfo().put("fileUrl", fileUrl);
					core.getLoginInfo().put("syncUrl", syncUrl);
					break;
				}
			}
			if (core.getLoginInfo().get("fileUrl") == null && core.getLoginInfo().get("syncUrl") == null) {
				core.getLoginInfo().put("fileUrl", url);
				core.getLoginInfo().put("syncUrl", url);
			}
			core.getLoginInfo().put("deviceid", "e" + String.valueOf(new Random().nextLong()).substring(1, 16)); // 生成15位随机数
			core.getLoginInfo().put("BaseRequest", new ArrayList<String>());
			String text = "";

			try {
				HttpEntity entity = MyHttpClient.getInstance().doGet(originalUrl, null, false, null);
				text = EntityUtils.toString(entity);
			} catch (Exception e) {
				LOG.info(e.getMessage());
				return;
			}
			//add by 默非默 2017-08-01 22:28:09
			//如果登录被禁止时，则登录返回的message内容不为空，下面代码则判断登录内容是否为空，不为空则退出程序
			String msg = getLoginMessage(text);
			if (!"".equals(msg)){
				LOG.info(msg);
			}
			Document doc = CommonTools.xmlParser(text);
			if (doc != null) {
				core.getLoginInfo().put(StorageLoginInfoEnum.skey.getKey(),
						doc.getElementsByTagName(StorageLoginInfoEnum.skey.getKey()).item(0).getFirstChild()
								.getNodeValue());
				core.getLoginInfo().put(StorageLoginInfoEnum.wxsid.getKey(),
						doc.getElementsByTagName(StorageLoginInfoEnum.wxsid.getKey()).item(0).getFirstChild()
								.getNodeValue());
				core.getLoginInfo().put(StorageLoginInfoEnum.wxuin.getKey(),
						doc.getElementsByTagName(StorageLoginInfoEnum.wxuin.getKey()).item(0).getFirstChild()
								.getNodeValue());
				core.getLoginInfo().put(StorageLoginInfoEnum.pass_ticket.getKey(),
						doc.getElementsByTagName(StorageLoginInfoEnum.pass_ticket.getKey()).item(0).getFirstChild()
								.getNodeValue());
			}

		}
	}
	
	private Map<String, List<String>> getPossibleUrlMap() {
		Map<String, List<String>> possibleUrlMap = new HashMap<String, List<String>>();
		possibleUrlMap.put("wx.qq.com", new ArrayList<String>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add("file.wx.qq.com");
				add("webpush.wx.qq.com");
			}
		});

		possibleUrlMap.put("wx2.qq.com", new ArrayList<String>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add("file.wx2.qq.com");
				add("webpush.wx2.qq.com");
			}
		});
		possibleUrlMap.put("wx8.qq.com", new ArrayList<String>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add("file.wx8.qq.com");
				add("webpush.wx8.qq.com");
			}
		});

		possibleUrlMap.put("web2.wechat.com", new ArrayList<String>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add("file.web2.wechat.com");
				add("webpush.web2.wechat.com");
			}
		});
		possibleUrlMap.put("wechat.com", new ArrayList<String>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add("file.web.wechat.com");
				add("webpush.web.wechat.com");
			}
		});
		return possibleUrlMap;
	}
	
	/**
	 * 解析登录返回的消息，如果成功登录，则message为空
	 * @param result
	 * @return
	 */
	public String getLoginMessage(String result){
		String[] strArr = result.split("<message>");
		String[] rs = strArr[1].split("</message>");
		if (rs!=null && rs.length>1) {
			return rs[0];
		}
		return "";
	}

}
