package com.schedule.service;

import com.schedule.dto.UserInfo;

public interface LoginService {
	public void register(UserInfo userInfo);
	public void login(UserInfo userInfo);
}
