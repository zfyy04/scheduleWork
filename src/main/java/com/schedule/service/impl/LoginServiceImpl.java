package com.schedule.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schedule.dao.LoginDao;
import com.schedule.dto.UserInfo;
import com.schedule.service.LoginService;

@Service
public class LoginServiceImpl implements LoginService {

	@Autowired
	public LoginDao loginDao;
	
	@Override
	public void register(UserInfo userInfo) {
		loginDao.insertUserInfo(userInfo);
	}

	@Override
	public void login(UserInfo userInfo) {

	}

}
