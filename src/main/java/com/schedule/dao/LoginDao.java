package com.schedule.dao;

import org.apache.ibatis.annotations.Mapper;

import com.schedule.dto.UserInfo;

@Mapper
public interface LoginDao {
	public void insertUserInfo(UserInfo userInfo);
}
