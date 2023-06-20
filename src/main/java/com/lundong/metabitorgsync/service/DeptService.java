package com.lundong.metabitorgsync.service;

import com.lundong.metabitorgsync.entity.KingdeeDept;

import java.util.List;

/**
 * @author RawChen
 * @date 2023-03-08 19:53
 */
public interface DeptService {

	/**
	 * 获取Kingdee部门列表
	 *
	 * @return
	 */
	List<KingdeeDept> queryDepartmentList();

	/**
	 * 修改Kingdee部门code
	 * @return
	 */
	boolean updateDepartment(String kingdeeDeptId, String code);

	/**
	 * 每天定时检查所有停用部门
	 */
	void syncStopDeptData();
}
