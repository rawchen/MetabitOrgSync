package com.lundong.metabitorgsync.service;

import com.lundong.metabitorgsync.entity.KingdeeDept;

import java.util.List;

/**
 * @author RawChen
 * @date 2023-03-08 19:53
 */
public interface DeptService {

	/**
	 * 获取SAP部门列表
	 *
	 * @return
	 */
	List<KingdeeDept> queryDepartmentList();
}
