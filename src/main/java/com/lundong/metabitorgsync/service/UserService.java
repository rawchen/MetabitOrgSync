package com.lundong.metabitorgsync.service;

import com.lundong.metabitorgsync.entity.KingdeeUser;

import java.util.List;

/**
 * @author RawChen
 * @date 2023-03-08 19:53
 */
public interface UserService {

	/**
	 * 获取SAP用户列表
	 *
	 * @return
	 */
	List<KingdeeUser> queryUserList();
}
