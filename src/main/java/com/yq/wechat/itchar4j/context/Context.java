package com.yq.wechat.itchar4j.context;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yq.wechat.itchar4j.beans.BaseMsg;
import com.yq.wechat.itchar4j.core.Core;

public enum Context {
	INSTANCE;
	
	private Logger LOG = LoggerFactory.getLogger(Context.class);
	
	private Context(){};
	
	/**核心类*/
	private Map<String, Core> cores = new ConcurrentHashMap<String, Core>();
	/**消息队列*/
	private LinkedBlockingQueue<BaseMsg> baseMsgQueue = new LinkedBlockingQueue<>();
	
	public Core getCore(String uuid){
		return cores.get(uuid);
	}
	
	public void addCore(Core core){
		String uuid = core.getUuid();
		if(uuid == null || uuid.length() <= 0){
			return;
		}
		cores.put(uuid, core);
	}
	
	public void removeCore(String uuid){
		cores.remove(uuid);
	}
	
	public Set<Entry<String, Core>> getCores(){
		return cores.entrySet();
	}
	
	public BaseMsg takeBaseMsg(){
		BaseMsg msg = null;
		try {
			msg = baseMsgQueue.take();
		} catch (InterruptedException e) {
			LOG.info("pollBaseMsg中断,有新消息被生产" + System.currentTimeMillis());
		}
		return msg;
	}
	
	public void putBaseMsg(BaseMsg baseMsg){
		try {
			baseMsgQueue.put(baseMsg);
		} catch (InterruptedException e) {
			LOG.info("offerBaseMsg中断,有新消息被消费" + System.currentTimeMillis());
		}
	}
}
