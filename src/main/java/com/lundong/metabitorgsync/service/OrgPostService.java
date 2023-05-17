package com.lundong.metabitorgsync.service;

import com.lundong.metabitorgsync.entity.KingdeeOrgPost;

import java.util.List;

/**
 * @author RawChen
 * @date 2023-05-17 17:11
 */
public interface OrgPostService {

	/**
	 * 获取Kingdee岗位信息列表（所有）
	 *
	 * @return
	 */
	List<KingdeeOrgPost> queryOrgPostList();

	/**
	 * 获取Kingdee岗位信息列表（过滤条件）
	 *
	 * @return
	 */
	List<KingdeeOrgPost> queryOrgPostList(String filterString);
}
