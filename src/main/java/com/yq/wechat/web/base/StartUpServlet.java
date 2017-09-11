package com.yq.wechat.web.base;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.yq.wechat.itchar4j.task.HandleMsgTask;
import com.yq.wechat.itchar4j.task.RevMsgTask;
import com.yq.wechat.web.constant.Constant;
import com.yq.wechat.web.task.CreateRoomTask;

@Component
public class StartUpServlet implements ApplicationListener<ContextRefreshedEvent>{
	private static final Logger LOG = LoggerFactory.getLogger(StartUpServlet.class);
	
	@Resource
	private Constant constant;
	@Resource
	private CreateRoomTask createRoomTask;
	
	public void init(){
		LOG.info("jsse.enableSNIExtension  false  防止SSL错误");
		System.setProperty("jsse.enableSNIExtension", "false"); // 防止SSL错误
		
		//开启线程 接收微信消息
		ExecutorService revMsg = Executors.newSingleThreadExecutor();
		revMsg.execute(new RevMsgTask());

		//开启线程 消费微信消息
		ExecutorService handlMsg = Executors.newFixedThreadPool(constant.getHandleMsgTaskThreadNum());
		for (int i = 0; i < constant.getHandleMsgTaskThreadNum(); i++) {
			handlMsg.execute(new HandleMsgTask());
		}
		//开启处理房间的线程
		ExecutorService creatrRoom = Executors.newSingleThreadExecutor();
		creatrRoom.execute(createRoomTask);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		init();
	}
}
