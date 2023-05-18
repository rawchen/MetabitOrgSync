package com.lundong.metabitorgsync.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.lundong.metabitorgsync.entity.*;
import com.lundong.metabitorgsync.mapper.DepartmentMapper;
import com.lundong.metabitorgsync.mapper.UserMapper;
import com.lundong.metabitorgsync.service.DeptService;
import com.lundong.metabitorgsync.service.SystemService;
import com.lundong.metabitorgsync.service.UserService;
import com.lundong.metabitorgsync.util.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SystemServiceImpl implements SystemService {
	private static final String DEPT_FILE_NAME = "部门信息.xlsx";

	@Autowired
	private DepartmentMapper departmentMapper;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private DeptService deptService;

	@Autowired
	private UserService userService;

	/**
	 * 初始化部门映射
	 *
	 * @return
	 */
	@Override
	@Transactional
	public String initDepartment() {
		// 初始化部门映射，只初始化一次
		long number = departmentMapper.selectCount(null);
		if (number > 0) {
			return "No synchronization";
		}

		// 获取飞书部门列表
		List<FeishuDept> depts = SignUtil.departments();

		// 查询金蝶云部门列表
		List<KingdeeDept> kingdeeDeptList = deptService.queryDepartmentList();

		// 构建部门映射关系
		if (CollectionUtil.isNotEmpty(depts) && CollectionUtil.isNotEmpty(kingdeeDeptList)) {
			Map<String, List<FeishuDept>> fsGroup = depts.stream().collect(Collectors.groupingBy(FeishuDept::getName));
			Map<String, List<KingdeeDept>> kingdeeGroup = kingdeeDeptList.stream().collect(Collectors.groupingBy(KingdeeDept::getName));
			// FeishuDept 与 KingdeeDept 合并为 Department
			List<Department> departments = fsGroup.entrySet().stream()
					.map(dept -> {
						String name = dept.getKey();
						FeishuDept feishuDept = dept.getValue().get(0);
						String kingdeeDeptId = null;
						String kingdeeParentId = null;
						String kingdeeDeptNumber = null;
						if (StrUtil.isNotBlank(name)) {
							List<KingdeeDept> list = kingdeeGroup.get(name);
							if (CollectionUtil.isNotEmpty(list)) {
								KingdeeDept kingdeeDept = list.get(0);
								kingdeeDeptId = kingdeeDept.getDeptId();
								kingdeeParentId = kingdeeDept.getParentId();
								kingdeeDeptNumber = kingdeeDept.getNumber();
							}
						}
						return Department.builder()
								.name(feishuDept.getName())
								.feishuDeptId(feishuDept.getDepartmentId())
								.feishuParentId(feishuDept.getParentDepartmentId())
								.kingdeeDeptId(kingdeeDeptId)
								.kingdeeParentId(kingdeeParentId)
								.number(kingdeeDeptNumber)
								.build();
					})
					.collect(Collectors.toList());
			if (CollectionUtil.isNotEmpty(departments)) {
				departmentMapper.insertBatch(departments);
			}
		}
		return "success";
	}

	/**
	 * 初始化用户映射
	 *
	 * @return
	 */
	@Override
	@Transactional
	public String initUser() {
		// 初始化用户映射，只初始化一次
		long number = userMapper.selectCount(null);
		if (number > 0) {
			return "No synchronization";
		}

		List<Department> departments = departmentMapper.selectAll();

		// 飞书用户列表查询（接口查）
		List<FeishuUser> feishuUsers = SignUtil.findByDepartment();

		// 替换部门ID和部门名称
		for (FeishuUser feishuUser : feishuUsers) {
			String deptIdAndName = SignUtil.getDepartmentIdAndName(feishuUser.getDepartmentId());
			System.out.println(deptIdAndName);
			String deptId = "";
			String deptName = "";
			if (deptIdAndName.contains(",")
					&& deptIdAndName.split(",")[0] != null
					&& deptIdAndName.split(",")[1] != null) {
				String[] split = deptIdAndName.split(",");
				deptId = split[0];
				deptName = split[1];
			}
			feishuUser.setDepartmentId(deptId);
			feishuUser.setDeptName(deptName);
		}

		// 部门匹配
		List<User> users = new ArrayList<>();

		for (FeishuUser feishuUser : feishuUsers) {
			User user = User.builder()
					.name(feishuUser.getName())
					.userId(feishuUser.getUserId())
					.build();
//            user.setKingdeeId(excelUser.getId());
//            user.setId(excelUser.getId());
			for (Department department : departments) {
				if (department.getName().equals(feishuUser.getDeptName())) {
					user.setDeptId(department.getFeishuDeptId());
					break;
				}
			}

			// Kingdee系统对应的人匹配
			List<KingdeeUser> kingdeeUsers = userService.queryUserList();
			for (KingdeeUser kingdeeUser : kingdeeUsers) {
				if (feishuUser.getName().equals(kingdeeUser.getName())) {
					user.setStaffId(kingdeeUser.getStaffId());
					user.setFid(kingdeeUser.getFid());
					break;
				}
			}

			users.add(user);
		}
		// 添加到用户映射表
		userMapper.insertBatch(users);

		return "success";
	}

}
