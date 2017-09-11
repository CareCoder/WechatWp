package com.yq.wechat.web.business.controller;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yq.wechat.web.business.beans.CreateRoomBean;
import com.yq.wechat.web.business.service.CoreService;
import com.yq.wechat.web.constant.StatusBean;
import com.yq.wechat.web.constant.StatusCode;
import com.yq.wechat.web.context.WebContext;

@Controller
@CrossOrigin(origins="*", maxAge=3600)
@RequestMapping("/wechat_wp")
public class CoreController {
	
//	private Logger LOG = LoggerFactory.getLogger(CoreController.class);
	
	@Resource
	private CoreService coreService;
	
	
	/**
	 * 登录
	 * @param userName
	 * @return
	 */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ResponseEntity<String> login(String userName){
		String res = coreService.login(userName);
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	/**
	 * 登出
	 * @param userName
	 * @return
	 */
	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public ResponseEntity<Integer> logout(String userName){
		int status = coreService.logout(userName);
		return new ResponseEntity<Integer>(status,  HttpStatus.OK);
	}
	
	/**
	 * 获取二维码的二进制数据
	 * @param userName
	 * @return
	 */
	@RequestMapping(value = "/getQR", method = RequestMethod.GET)
	public ResponseEntity<String> getQR(String userName){
		String qrUrl = coreService.getQR(userName);
		return new ResponseEntity<String>(qrUrl, HttpStatus.OK);
	}
	
	/**
	 * 获取登录信息,用户是否扫描二维码 并且登录了
	 * @param userName
	 * @return
	 */
	@RequestMapping(value = "/getLoginInfo", method = RequestMethod.GET)
	public ResponseEntity<String> getLoginInfo(String userName){
		String infoCode = coreService.getLoginInfo(userName);
		return new ResponseEntity<String>(infoCode, HttpStatus.OK);
	}
	
	/**
	 * 获取玩家自动创建房间的信息
	 * @param userName
	 * @return
	 */
	@RequestMapping(value = "/getCreateRoomInfo", method = RequestMethod.GET)
	public ResponseEntity<String> getCreateRoomInfo(String userName){
		String infoCode = coreService.getCreateRoomInfo(userName);
		return new ResponseEntity<String>(infoCode, HttpStatus.OK);
	}
	
	/**
	 * 用户登录成功后获取用户的所有数据
	 * @param userName
	 * @return
	 */
	@RequestMapping(value = "/getUserInfo", method = RequestMethod.GET)
	public ResponseEntity<String> getUserInfo(String userName){
		JSONObject jb = coreService.getUserInfo(userName);
		String res = "";
		if(jb != null){
			res = jb.toJSONString();
		}
		return new ResponseEntity<String>(res, HttpStatus.OK);
	}
	
	/**
	 * 自动创建房间
	 * @param userName 创建房间的id
	 * @param groupId 群id @@开头的
	 * @param mjType 麻将类型 比如自贡麻将是2
	 * @param option 创建麻将的选项
	 * @param accid 玩家在游戏中的id
	 * @return
	 */
	@RequestMapping(value = "/autoCreateRoom", method = RequestMethod.GET)
	public ResponseEntity<String> autoCreateRoom(CreateRoomBean createRoomBean){
		boolean validate = coreService.validateCreateRoom(createRoomBean);
		int statusCode = StatusCode.ERROR;
		String statusInfo = "fail";
		if(validate){
			WebContext.INSTANCE.addCreateRoomBean(createRoomBean);
			statusCode = StatusCode.SUCCESS;
			statusInfo = "success";
		}
		StatusBean sb = new StatusBean(statusCode, statusInfo);
		return new ResponseEntity<String>(JSON.toJSONString(sb), HttpStatus.OK);
	}
	
	/**
	 * 取消创建房间任务
	 * @param userName
	 * @param groupId
	 * @return
	 */
	@RequestMapping(value = "/cancelCreateRoom", method = RequestMethod.GET)
	public ResponseEntity<Integer> cancelCreateRoom(String userName, String groupId){
		int statusCode = coreService.cancelCreateRoom(userName, groupId);
		return new ResponseEntity<Integer>(statusCode, HttpStatus.OK);
	}
}
