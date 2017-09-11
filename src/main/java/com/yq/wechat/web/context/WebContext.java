package com.yq.wechat.web.context;

import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.yq.wechat.web.business.beans.CreateRoomBean;

/**
 * web端的上下文
 * @author yq
 *
 */
public enum WebContext {
	INSTANCE;
	
//	private Logger LOG = LoggerFactory.getLogger(WebContext.class);
	
	private WebContext(){};
	
	/**用户的帐号与微信核心类uuid对应关系*/
	private ConcurrentHashMap<String, String> user2Core = new ConcurrentHashMap<>();
	
	/**自动创建房间 key通过value的 userName_groupId*/
	private ConcurrentHashMap<String, ConcurrentHashMap<String, CreateRoomBean>> createRoomBeans = new ConcurrentHashMap<>();
	/**
	 * 通过用户名找到对应的core的uuid
	 * @param userName 用户名
	 * @return uuid
	 */
	public String getCoreUuid(String userName){
		return user2Core.get(userName);
	}
	
	/**
	 * 添加用户和core的对应关系
	 * @param userName 用户名
	 * @param coreUuid uuid
	 */
	public void addUser2Core(String userName, String coreUuid){
		user2Core.put(userName, coreUuid);
	}
	/**
	 * 删除玩家对应core的联系
	 * @param userName
	 * @return
	 */
	public String removeUser2Core(String userName){
		return user2Core.remove(userName);
	}
	
	/**
	 * 删除指定玩家,指定群的自动创建房间任务
	 * @param userName
	 * @param groupId
	 * @return
	 */
	public CreateRoomBean removeCreateRoomBean(String userName, String groupId){
		ConcurrentHashMap<String, CreateRoomBean> map = createRoomBeans.get(userName);
		CreateRoomBean removed = null;
		if(map != null){
			removed = map.remove(groupId);
			if(map.isEmpty()){
				createRoomBeans.remove(userName);
			}
		}
		return removed;
	}
	
	/**
	 * 删除当前用户所有的自动创建房间任务
	 * @param userName
	 * @return
	 */
	public ConcurrentHashMap<String, CreateRoomBean> removeCreateRoomBean(String userName){
		return createRoomBeans.remove(userName);
	}
	
	/**
	 * 增加玩家自动创建房间任务
	 * @param createRoomBean
	 */
	public void addCreateRoomBean(CreateRoomBean createRoomBean){
		String userName = createRoomBean.getUserName();
		String groupId = createRoomBean.getGroupId();
		ConcurrentHashMap<String, CreateRoomBean> map = createRoomBeans.get(userName);
		if(map == null){
			map = new ConcurrentHashMap<String, CreateRoomBean>();
		}
		map.put(groupId, createRoomBean);
		createRoomBeans.put(userName, map);
	}
	
	/**
	 * 得到玩家所有创建房间的任务
	 * @return
	 */
	public Set<Entry<String, ConcurrentHashMap<String, CreateRoomBean>>> getCreateRoomBeans(){
		return createRoomBeans.entrySet();
	}
	
	public ConcurrentHashMap<String, CreateRoomBean> getCreateRoomBeans(String userName){
		return createRoomBeans.get(userName);
	}
}
