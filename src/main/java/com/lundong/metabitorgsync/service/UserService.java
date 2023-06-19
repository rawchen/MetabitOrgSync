package com.lundong.metabitorgsync.service;

import com.lundong.metabitorgsync.entity.KingdeePerson;
import com.lundong.metabitorgsync.entity.KingdeeUser;

import java.util.List;

/**
 * @author RawChen
 * @date 2023-03-08 19:53
 */
public interface UserService {

	/**
	 * 获取Kingdee用户列表
	 *
	 * @return
	 */
	List<KingdeeUser> queryUserList();

	/**
	 * 获取Kingdee人员详细信息(公共)表列表
	 *
	 * @param filterString
	 * @return
	 */
	List<KingdeePerson> queryPersonList(String filterString);
}
