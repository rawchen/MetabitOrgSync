package com.lundong.metabitorgsync.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.lundong.metabitorgsync.entity.*;
import com.lundong.metabitorgsync.mapper.DepartmentMapper;
import com.lundong.metabitorgsync.mapper.UserMapper;
import com.lundong.metabitorgsync.service.DeptService;
import com.lundong.metabitorgsync.service.SystemService;
import com.lundong.metabitorgsync.service.UserService;
import com.lundong.metabitorgsync.util.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
            Map<String, List<KingdeeDept>> sapGroup = kingdeeDeptList.stream().collect(Collectors.groupingBy(KingdeeDept::getName));
            // ExcelDept 与 SapDept 合并为 Department
            List<Department> departments = fsGroup.entrySet().stream()
                    .map(dept -> {
                        String name = dept.getKey();
                        FeishuDept feishuDept = dept.getValue().get(0);
                        String sapDeptId = null;
                        String sapParentId = null;
                        String pkId = null;
                        if (StrUtil.isNotBlank(name)) {
                            List<KingdeeDept> list = sapGroup.get(name);
                            if (CollectionUtil.isNotEmpty(list)) {
                                KingdeeDept kingdeeDept = list.get(0);
                                sapDeptId = kingdeeDept.getDeptId();
                                sapParentId = kingdeeDept.getDeptId();
                                pkId = kingdeeDept.getPkId();
                            }
                        }
                        return Department.builder()
                                .name(feishuDept.getName())
                                .feishuDeptId(feishuDept.getDepartmentId())
                                .feishuParentId(feishuDept.getParentDepartmentId())
                                .sapDeptId(sapDeptId)
                                .sapParentId(sapParentId)
                                .pkId(pkId)
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
            User user = new User();
//            user.setSapId(excelUser.getId());
            user.setName(feishuUser.getName());
//            user.setId(excelUser.getId());
            user.setUserId(feishuUser.getUserId());
            for (Department department : departments) {
                if (department.getName().equals(feishuUser.getDeptName())) {
                    user.setDeptId(department.getFeishuDeptId());
                    break;
                }
            }

            // SAP系统对应的人匹配
            List<KingdeeUser> kingdeeUsers = userService.queryUserList();
            for (KingdeeUser kingdeeUser : kingdeeUsers) {
                if (feishuUser.getName().equals(kingdeeUser.getName())) {
                    user.setPkId(kingdeeUser.getPkId());
                    break;
                }
            }

            users.add(user);
        }
        // 添加到用户映射表
        userMapper.insertBatch(users);

        return "success";
    }

    public List<ExcelDept> parseExcel() {
        List<ExcelDept> departments = new ArrayList<>();
        try {
            ClassPathResource classPathResource = new ClassPathResource(DEPT_FILE_NAME);
            EasyExcel.read(classPathResource.getInputStream(), ExcelDept.class, new AnalysisEventListener<ExcelDept>() {
                @Override
                public void invoke(ExcelDept dept, AnalysisContext context) {
                    departments.add(dept);
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().headRowNumber(2).doRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 处理部门父子关系
        for (ExcelDept dept : departments) {
            if (dept.getName().contains("|")) {
                dept.setName(dept.getName().substring(0, dept.getName().indexOf("|")));
            }
            dept.setName(dept.getName().substring(dept.getName().indexOf("/")));
            // 过滤完如果只有斜杠则为第一级，设置parentId为sap系统的10
            String str = dept.getName().replace("/", "");
            if (dept.getName().length() - str.length() == 1) {
                dept.setParentId("10");
            }
        }
        // ExcelDept 根据/数量少到多排序
        Collections.sort(departments);

        // 递归找 parentID
        matchParentId(departments);

        // 去除多余层级
        for (ExcelDept newExcelDept : departments) {
            String temp;
            temp = newExcelDept.getName().substring(newExcelDept.getName().lastIndexOf("/") + 1);
            newExcelDept.setName(temp);
        }
        return departments;
    }

    /**
     * 匹配层级关系
     *
     * @param departments 部门列表
     */
    private void matchParentId(List<ExcelDept> departments) {
        for (int i = 0; i < departments.size(); i++) {
            // 如果斜杠大于一个就需要去找父ID
            if (departments.get(i).getName().length() - departments.get(i).getName().replace("/", "").length() > 1) {
                // 往上找斜杠数量与之不同的，就是它的parent
                int tempIndex = i;
                while (tempIndex > 0) {
                    String name = departments.get(i).getName();
                    String tempName = departments.get(tempIndex - 1).getName();
                    String sub = name.substring(0, name.lastIndexOf("/"));
                    if (tempName.equals(sub)) {
                        // 如果截取开头到最后一个/前一个字符，与上面name匹配则为
                        departments.get(i).setParentId(departments.get(tempIndex - 1).getId());
                        break;
                    } else {
                        tempIndex--;
                    }
                }
            }
        }
    }


}
