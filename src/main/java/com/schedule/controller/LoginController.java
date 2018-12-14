package com.schedule.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.schedule.dto.UserInfo;
import com.schedule.service.LoginService;

@Controller
public class LoginController {
	
	@Autowired
	public LoginService loginService;
	
	/**
	 * 方式1返回页面，通过string指定返回文件路径
	 * @return
	 */
	@RequestMapping(value="/")
	public String login(){
		return "login/login";
	}
	
	/**
	 * 方式2返回页面，通过ModelAndView进行页面操作返回
	 */
	@RequestMapping(value="/index")
	public ModelAndView loginJsp(){
		ModelAndView mv = new ModelAndView("login/login");
		return mv;
	}
	
	@RequestMapping(value="/login",method=RequestMethod.GET)
	@ResponseBody
	public Map<String, String> login_in(ModelAndView model,HttpServletRequest request){
		Map<String, String> retMap = new HashMap<String, String>();
		retMap.put("successFlag", "Y");
		retMap.put("userId", request.getParameter("userId"));
		return retMap;
	}
	
	@RequestMapping(value="/register",method=RequestMethod.POST)
	@ResponseBody
	public Map<String, String> register(ModelAndView model,HttpServletRequest request){
		String userId = request.getParameter("userId");
		String userName = request.getParameter("userName");
		String passWord = request.getParameter("passWord");
		String mobile = request.getParameter("mobile");
		UserInfo userInfo = new UserInfo();
		userInfo.setUserId(userId);
		userInfo.setUserName(userName);
		userInfo.setPassWord(passWord);
		userInfo.setMobile(mobile);
		
		loginService.register(userInfo);
		
		Map<String, String> retMap = new HashMap<String, String>();
		retMap.put("successFlag", "Y");
		retMap.put("userId", request.getParameter("userId"));
		return retMap;
	}
	
}
