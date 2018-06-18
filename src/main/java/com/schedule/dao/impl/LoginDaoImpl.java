package com.schedule.dao.impl;

import org.springframework.stereotype.Repository;

import com.schedule.dao.LoginDao;
import com.schedule.dto.UserInfo;

@Repository
public class LoginDaoImpl implements LoginDao {

	@Override
	public void insertUserInfo(UserInfo userInfo) {
		System.out.println(userInfo.getUserId());
	}
}
