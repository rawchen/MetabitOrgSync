package com.lundong.metabitorgsync.controller;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.sdk.servlet.ext.ServletAdapter;
import com.lark.oapi.service.contact.v3.ContactService;
import com.lark.oapi.service.contact.v3.model.*;
import com.lundong.metabitorgsync.config.Constants;
import com.lundong.metabitorgsync.entity.Department;
import com.lundong.metabitorgsync.entity.ExcelDept;
import com.lundong.metabitorgsync.entity.SapDept;
import com.lundong.metabitorgsync.entity.User;
import com.lundong.metabitorgsync.mapper.DepartmentMapper;
import com.lundong.metabitorgsync.mapper.UserMapper;
import com.lundong.metabitorgsync.service.DeptService;
import com.lundong.metabitorgsync.service.SystemService;
import com.lundong.metabitorgsync.util.SignUtil;
import com.lundong.metabitorgsync.util.StringUtil;
import com.lundong.metabitorgsync.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RawChen
 * @date 2023-03-02 11:19
 */
@Slf4j
@RestController
@RequestMapping
public class EventController {

    @Autowired
    private ServletAdapter servletAdapter;

    @Autowired
    DepartmentMapper departmentMapper;

    @Autowired
    UserMapper userMapper;

    @Autowired
    private DeptService deptService;

    @Autowired
    private SystemService systemService;

    // 注册消息处理器
    private final EventDispatcher EVENT_DISPATCHER = EventDispatcher
            .newBuilder(Constants.VERIFICATION_TOKEN, Constants.ENCRYPT_KEY)
            .onP2UserCreatedV3(new ContactService.P2UserCreatedV3Handler() {
                // 用户创建
                @Override
                public void handle(P2UserCreatedV3 event) {
                    log.info("P2UserCreatedV3: {}", Jsons.DEFAULT.toJson(event));
                    // 处理用户创建事件
                    // 1.获取处理订阅消息体
                    JSONObject requestJson = new JSONObject();
                    String resultJson = Jsons.DEFAULT.toJson(event);
                    JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
                    JSONObject eventObject = (JSONObject) eventsObject.get("event");
                    JSONObject object = (JSONObject) eventObject.get("object");
                    String user_id = object.getString("user_id");
                    String name = object.getString("name");
                    String en_name = object.getString("en_name");
                    String mobile = object.getString("mobile");
                    String email = object.getString("email");
                    JSONArray department_ids = (JSONArray) object.get("department_ids");
                    String employee_no = object.getString("employee_no");
                    String gender = object.getString("gender");
                    String city = object.getString("city");
//                    String leader_user_id = object.getString("leader_user_id");
                    String employee_type = object.getString("employee_type");
                    String join_time = object.getString("join_time");
                    String job_title = object.getString("job_title");
                    String nickname = object.getString("nickname");

                    requestJson.put("ComCode", "4");
                    requestJson.put("lastName", name);
                    requestJson.put("EnglishName1", en_name);
                    requestJson.put("mobile", StringUtil.mobileDivAreaCode(mobile));
                    requestJson.put("email", email);

                    // 判断这个用户在映射表是否已经存在（防止事件流重复订阅）
                    User userTemp = userMapper.selectOne(new LambdaQueryWrapper<User>()
                            .eq(User::getUserId, user_id).last("limit 1"));
                    if (userTemp != null) {
                        log.info("P2UserCreatedV3: 添加失败，重复添加用户：" + name);
                        return;
                    }

                    // 拿到飞书-SAP映射的部门id
                    log.info("department_ids: {}", department_ids.getString(0));
                    String deptIdAndName = SignUtil.getDepartmentIdAndName(department_ids.getString(0));
                    String deptId = "";
                    if (deptIdAndName.contains(",")) {
                        String[] split = deptIdAndName.split(",");
                        deptId = split[0];
                    }
                    Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, deptId).last("limit 1"));
                    if (department != null && department.getSapDeptId() != null) {
                        requestJson.put("EmDept", department.getSapDeptId());
                    } else {
                        requestJson.put("EmDept", "0");
                    }
                    requestJson.put("JobNum", employee_no);
                    requestJson.put("sex", "1".equals(gender) ? "F" : "M");
                    requestJson.put("city", city);
                    //                    requestJson.put("Leader", leader_user_id);
                    requestJson.put("Status", StringUtil.employeeConvert(employee_type));                    // A正式B离职C试用
                    requestJson.put("TimeOfEntry", TimeUtil.timestampToUTC(join_time));
                    requestJson.put("jobTitle", job_title);
                    requestJson.put("EnglishName", nickname);
                    requestJson.put("RowStatus", "A");
                    requestJson.put("DocEntry", "219");
                    String requestJsonAddArg = "{\"U_OHEM\":[" + requestJson.toJSONString() + "],}";

                    // 2.接口参数处理
                    String timestamp = TimeUtil.getTimestamp();
                    StringBuilder url = new StringBuilder();
                    Map<String, String> objects = new HashMap<>();
                    objects.put("APPID", Constants.APPID);
                    objects.put("COMPANYID", Constants.COMPANYID);
                    objects.put("TIMESTAMP", timestamp);
                    objects.put("FORMID", Constants.FORM_ID_USER);
                    String md5Token = SignUtil.makeMd5Token(objects, Constants.SECRETKEY, requestJsonAddArg);
                    url.append(Constants.DOMAIN_PORT).append(Constants.ADD)
                            .append("/").append(Constants.FORM_ID_USER)
                            .append("/").append(timestamp).append("/").append(md5Token);

                    // 3.调用SAP接口
                    try {
                        String resultStr = HttpRequest.post(url.toString())
                                .body(requestJsonAddArg)
                                .execute()
                                .body();
                        log.info("r: {}", resultStr);
                        if (StringUtils.isNotEmpty(resultStr)) {
                            JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
                            String resultCode = resultObject.getString("Code");
                            if (StringUtils.isNotEmpty(resultCode) && "0".equals(resultCode)) {
                                // 新增SAP用户成功后添加用户到映射表（包含DocEntry）
                                User user = new User();
                                user.setName(name);
                                user.setDocEntry(resultObject.getString("Result"));
                                //                                user.setSapId();
                                user.setUserId(user_id);
                                user.setDeptId(deptId);
                                userMapper.insert(user);

                                log.info("success: {}", resultStr);
                            } else {
                                log.info("fail: {}", resultStr);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).onP2UserUpdatedV3(new ContactService.P2UserUpdatedV3Handler() {
                // 用户修改
                @Override
                public void handle(P2UserUpdatedV3 event) throws Exception {
                    log.info("P2UserUpdatedV3: {}", Jsons.DEFAULT.toJson(event));
                    JSONObject requestJson = new JSONObject();
                    String resultJson = Jsons.DEFAULT.toJson(event);
                    JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
                    JSONObject eventObject = (JSONObject) eventsObject.get("event");
                    JSONObject object = (JSONObject) eventObject.get("object");
                    String user_id = object.getString("user_id");
                    String name = object.getString("name");
                    String en_name = object.getString("en_name");
                    String mobile = object.getString("mobile");
                    String email = object.getString("email");
                    JSONArray department_ids = (JSONArray) object.get("department_ids");
                    String employee_no = object.getString("employee_no");
                    String gender = object.getString("gender");
                    String city = object.getString("city");
//                    String leader_user_id = object.getString("leader_user_id");
                    String employee_type = object.getString("employee_type");
                    String join_time = object.getString("join_time");
                    String job_title = object.getString("job_title");
                    String nickname = object.getString("nickname");

                    requestJson.put("ComCode", "4");
                    requestJson.put("lastName", name);
                    requestJson.put("EnglishName1", en_name);
                    requestJson.put("mobile", StringUtil.mobileDivAreaCode(mobile));
                    requestJson.put("email", email);

                    // 拿到飞书-SAP映射的部门id
                    log.info("department_ids: {}", department_ids.getString(0));
                    String deptIdAndName = SignUtil.getDepartmentIdAndName(department_ids.getString(0));
                    String deptId = "";
                    if (deptIdAndName.contains(",")) {
                        String[] split = deptIdAndName.split(",");
                        deptId = split[0];
                    }
                    Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, deptId).last("limit 1"));
                    if (department != null && department.getSapDeptId() != null) {
                        requestJson.put("EmDept", department.getSapDeptId());
                    } else {
                        requestJson.put("EmDept", "0");
                    }

                    // 根据飞书user_id查询sap单据id（映射好的用户数据库查）
                    User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserId, user_id).last("limit 1"));
                    String docEntry = "";
                    if (user != null && user.getDocEntry() != null) {
                        docEntry = user.getDocEntry();
                    } else {
                        docEntry = "0";
                        log.info("用户修改事件查询不到用户DocEntry: {}", user_id);
                    }

                    requestJson.put("JobNum", employee_no);
                    requestJson.put("sex", "1".equals(gender) ? "F" : "M");
                    requestJson.put("city", city);
//                    requestJson.put("Leader", leader_user_id);
                    requestJson.put("Status", StringUtil.employeeConvert(employee_type));                    // A正式B离职C试用
                    requestJson.put("TimeOfEntry", TimeUtil.timestampToUTC(join_time));
                    requestJson.put("jobTitle", job_title);
                    requestJson.put("EnglishName", nickname);
                    requestJson.put("RowStatus", "U");
                    String requestJsonAddArg = "{\"U_OHEM\":[" + requestJson.toJSONString() + "],}";

                    // 2.接口参数处理
                    String timestamp = TimeUtil.getTimestamp();
                    StringBuilder url = new StringBuilder();
                    Map<String, String> objects = new HashMap<>();
                    objects.put("APPID", Constants.APPID);
                    objects.put("COMPANYID", Constants.COMPANYID);
                    objects.put("TIMESTAMP", timestamp);
                    objects.put("FORMID", Constants.FORM_ID_USER);
                    objects.put("DOCENTRY", docEntry);
                    String md5Token = SignUtil.makeMd5Token(objects, Constants.SECRETKEY, requestJsonAddArg);
                    url.append(Constants.DOMAIN_PORT).append(Constants.UPDATE)
                            .append("/").append(Constants.FORM_ID_USER)
                            .append("/").append(docEntry)
                            .append("/").append(timestamp).append("/").append(md5Token);
                    log.info("修改用户事件url: {}", url);
                    // 3.调用SAP接口
                    String resultStr = HttpRequest.post(url.toString())
                            .body(requestJsonAddArg).execute().body();
                    log.info("r: {}", resultStr);
                    if (StringUtils.isNotEmpty(resultStr)) {
                        JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
                        String resultCode = resultObject.getString("Code");
                        if (StringUtils.isNotEmpty(resultCode) && "0".equals(resultCode)) {
                            log.info("success: {}", resultStr);
                        } else {
                            log.info("fail: {}", resultStr);
                        }
                    }

                    // 根据用户ID查询用户映射表，如果对应的找到用户，就更新用户映射表的部门id最新，名称最新
                    if (user != null) {
                        User userTemp = new User();
                        userTemp.setId(user.getId());
                        userTemp.setName(name);
                        userTemp.setDeptId(deptId);
                        userMapper.updateById(userTemp);
                    }

                }
            }).onP2UserDeletedV3(new ContactService.P2UserDeletedV3Handler() {
                // 用户删除
                @Override
                public void handle(P2UserDeletedV3 event) throws Exception {
                    log.info("P2UserDeletedV3: {}", Jsons.DEFAULT.toJson(event));
                    // 准备docEntry和飞书user_id
                    String resultJson = Jsons.DEFAULT.toJson(event);
                    JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
                    JSONObject eventObject = (JSONObject) eventsObject.get("event");
                    JSONObject object = (JSONObject) eventObject.get("object");
                    String user_id = object.getString("user_id");
                    User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserId, user_id).last("limit 1"));
                    String docEntry = "";
                    if (user != null && user.getDocEntry() != null) {
                        docEntry = user.getDocEntry();
                    } else {
                        docEntry = "0";
                        log.info("用户删除事件查询不到用户DocEntry: {}", user_id);
                    }

                    // SAP删除，根据映射表的docEntry
                    StringBuilder url = new StringBuilder();
                    String timestamp = TimeUtil.getTimestamp();
                    Map<String, String> objects = new HashMap<>();
                    objects.put("APPID", Constants.APPID);
                    objects.put("COMPANYID", Constants.COMPANYID);
                    objects.put("FORMID",   Constants.FORM_ID_USER);
                    objects.put("TIMESTAMP", timestamp);
                    objects.put("DOCENTRY", docEntry);
                    JSONObject requestJson = new JSONObject();
                    requestJson.put("ComCode", "4");
//                    requestJson.put("lastName", "1");
//                    requestJson.put("EnglishName1", "1");
//                    requestJson.put("mobile", "1");
//                    requestJson.put("email", "123@qq.com");
//                    requestJson.put("EmDept", "123");
                    requestJson.put("Status", "B");
                    requestJson.put("RowStatus", "D");
                    String requestJsonAddArg = "{\"U_OHEM\":[" + requestJson.toJSONString() + "],}";
                    String md5Token = SignUtil.makeMd5Token(objects, Constants.SECRETKEY, requestJsonAddArg);
                    url.append(Constants.DOMAIN_PORT).append(Constants.UPDATE)
                            .append("/").append(Constants.FORM_ID_USER)
                            .append("/").append(docEntry)
                            .append("/").append(timestamp)
                            .append("/").append(md5Token);
                    System.out.println("url:" + url);
                    try {
                        String s = HttpRequest.post(url.toString())
                                .body(requestJsonAddArg)
                                .execute()
                                .body();
                        System.out.println("r:" + s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // 关系映射表删除根据user_id
                    userMapper.deleteById(user.getId());

                }
            })
            .onP2DepartmentCreatedV3(new ContactService.P2DepartmentCreatedV3Handler() {
                // 部门创建
                @Override
                public void handle(P2DepartmentCreatedV3 event) throws Exception {
                    log.info("P2DepartmentCreatedV3: {}", Jsons.DEFAULT.toJson(event));
                    String resultJson = Jsons.DEFAULT.toJson(event);
                    JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
                    JSONObject eventObject = (JSONObject) eventsObject.get("event");
                    JSONObject object = (JSONObject) eventObject.get("object");
                    String name = object.getString("name");
                    String department_id = object.getString("department_id");
                    String parent_department_id = object.getString("parent_department_id"); // od-b66de0fbb2edb71b7f5b020d675a3e04

                    // 调用自建应用根据父部门id：od-xxx获取部门名和导出的id样例
                    String deptIdAndName = SignUtil.getDepartmentIdAndName(parent_department_id);
                    String deptParentId = "";
                    String deptParentName = "";
                    if (deptIdAndName.startsWith("0,")) {
                        deptParentId = "10";
                        deptParentName = "深圳市联创杰科技有限公司";
                    } else if (deptIdAndName.contains(",")
                            && deptIdAndName.split(",")[0] != null
                            && deptIdAndName.split(",")[1] != null) {
                        String[] split = deptIdAndName.split(",");
                        deptParentId = split[0];
                        deptParentName = split[1];
                    } else {
                        deptParentId = "";
                        deptParentName = "";
                    }

                    // SAP新增部门
                    String formID = "-96134";
                    String requestJson = "{\n" +
                            "    \"U_OEPT\":[\n" +
                            "        {\n" +
                            "            \"DocEntry\":1,\n" +
                            "            \"RowStatus\":\"A\"\n" +
                            "        }\n" +
                            "    ],\n" +
                            "    \"U_EPT1\":[\n" +
                            "        {\n" +
                            "            \"ParentName\":\"" + deptParentName + "\",\n" +
                            "            \"DeptName\":\"" + name + "\",\n" +
                            "            \"LineNum\":1,\n" +
                            "            \"RowStatus\":\"A\",\n" +
                            "            \"DocEntry\":2\n" +
                            "        }\n" +
                            "    ]\n" +
                            "}";
                    StringBuilder url = new StringBuilder();
                    String timestamp = TimeUtil.getTimestamp();
                    Map<String, String> objects = new HashMap<>();
                    objects.put("APPID", Constants.APPID);
                    objects.put("COMPANYID", Constants.COMPANYID);
                    objects.put("FORMID", formID);
                    objects.put("TIMESTAMP", timestamp);
                    String md5Token = SignUtil.makeMd5Token(objects, Constants.SECRETKEY, requestJson);
                    url.append(Constants.DOMAIN_PORT).append(Constants.ADD)
                            .append("/").append(formID)
                            .append("/").append(timestamp)
                            .append("/").append(md5Token);
                    System.out.println("url:" + url);
                    String s = HttpRequest.post(url.toString())
                            .body(requestJson)
                            .execute()
                            .body();
                    System.out.println("r:" + s);

                    // 插入部门后解析Result出来的docEntry，去SAP系统部门列表遍历拿到
                    String deptCode = "";
                    String parentCode = "";
                    JSONObject result = (JSONObject) JSONObject.parse(s);
                    String resultDocEntry = result.getString("Result");
                    if (resultDocEntry != null) {
                        List<SapDept> sapDepts = deptService.queryDepartmentList();
                        for (SapDept sapDept : sapDepts) {
                            if (sapDept.getDocEntry().equals(resultDocEntry)) {
                                parentCode = sapDept.getParentId();
                                deptCode = sapDept.getId();
                                break;
                            }
                        }
                    }

                    // 映射表新增部门
                    Department department = Department.builder()
                            .name(name)
                            .feishuDeptId(department_id)
                            .feishuParentId(deptParentId)
                            .sapParentId(parentCode)
                            .sapDeptId(deptCode)
                            .docEntry(resultDocEntry)
                            .build();
                    departmentMapper.insert(department);

                }
            }).onP2DepartmentUpdatedV3(new ContactService.P2DepartmentUpdatedV3Handler() {
                // 部门修改
                @Override
                public void handle(P2DepartmentUpdatedV3 event) throws Exception {
                    log.info("P2DepartmentUpdatedV3: {}", Jsons.DEFAULT.toJson(event));
                    // 根据department_id查询Department与docEntry
                    String resultJson = Jsons.DEFAULT.toJson(event);
                    JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
                    JSONObject eventObject = (JSONObject) eventsObject.get("event");
                    JSONObject object = (JSONObject) eventObject.get("object");
                    String name = object.getString("name");
                    String department_id = object.getString("department_id");
                    String parent_department_id = object.getString("parent_department_id");

                    Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, department_id).last("limit 1"));
                    String docEntry = "";
                    if (department != null && department.getDocEntry() != null) {
                        docEntry = department.getDocEntry();
                    } else {
                        docEntry = "0";
                        log.info("部门修改事件查询不到用户DocEntry: {}", department_id);
                    }

                    // 根据parent_department_id查询原来映射表的部门
                    String deptIdAndName = SignUtil.getDepartmentIdAndName(parent_department_id);
                    String deptId = "";
                    if (deptIdAndName.contains(",")) {
                        String[] split = deptIdAndName.split(",");
                        deptId = split[0];
                    }
                    String departmentParentName = "";
                    Department departmentParent = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, deptId).last("limit 1"));
                    if (department != null && department.getSapDeptId() != null) {
                        departmentParentName = departmentParent.getName();
                        // 修改
                    }
                    // 修改SAP系统部门的名称ParentName和DeptName
                    StringBuilder url = new StringBuilder();
                    String timestamp = TimeUtil.getTimestamp();
                    Map<String, String> objects = new HashMap<>();
                    objects.put("APPID", Constants.APPID);
                    objects.put("COMPANYID", Constants.COMPANYID);
                    objects.put("FORMID", Constants.FORM_ID_DEPT);
                    objects.put("TIMESTAMP", timestamp);
                    objects.put("DOCENTRY",docEntry);
                    String requestJson = "{\n" +
                    "    \"U_OEPT\":[\n" +
                            "        {\n" +
                            "            \"DocEntry\":" + docEntry + ",\n" +
                            "            \"RowStatus\":\"U\"\n" +
                            "        }\n" +
                            "    ],\n" +
                            "    \"U_EPT1\":[\n" +
                            "        {\n" +
                            "            \"ParentName\":\"" + departmentParentName + "\",\n" +
                            "            \"DeptName\":\"" + name + "\",\n" +
                            "            \"LineNum\":1,\n" +
                            "            \"RowStatus\":\"U\"\n" +
                            "        },\n" +
                            "    ]\n" +
                            "}";
                    String md5Token = SignUtil.makeMd5Token(objects, Constants.SECRETKEY, requestJson);
                    url.append(Constants.DOMAIN_PORT).append(Constants.UPDATE)
                            .append("/").append(Constants.FORM_ID_DEPT)
                            .append("/").append(docEntry)
                            .append("/").append(timestamp)
                            .append("/").append(md5Token);
                    System.out.println("url:" + url);
                    String s = HttpRequest.post(url.toString())
                            .body(requestJson)
                            .execute()
                            .body();
                    System.out.println("r:" + s);

                    // 修改部门映射表，名称，飞书父id，sap父id
                    department.setName(name);
                    department.setFeishuParentId(deptId);
                    department.setSapParentId(departmentParent.getSapDeptId());
                    departmentMapper.updateById(department);

                }
            }).onP2DepartmentDeletedV3(new ContactService.P2DepartmentDeletedV3Handler() {
                // 部门删除
                @Override
                public void handle(P2DepartmentDeletedV3 event) throws Exception {
                    log.info("P2DepartmentDeletedV3: {}", Jsons.DEFAULT.toJson(event));
                    // 准备docEntry和飞书department_id
                    String resultJson = Jsons.DEFAULT.toJson(event);
                    JSONObject eventsObject = (JSONObject) JSONObject.parse(resultJson);
                    JSONObject eventObject = (JSONObject) eventsObject.get("event");
                    JSONObject object = (JSONObject) eventObject.get("object");
                    String department_id = object.getString("department_id");
                    Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, department_id).last("limit 1"));
                    String docEntry = "";
                    if (department != null && department.getDocEntry() != null) {
                        docEntry = department.getDocEntry();
                    } else {
                        docEntry = "0";
                        log.info("部门删除事件查询不到用户DocEntry: {}", department_id);
                    }

                    // SAP删除，根据映射表的docEntry
                    StringBuilder url = new StringBuilder();
                    String timestamp = TimeUtil.getTimestamp();
                    Map<String, String> objects = new HashMap<>();
                    objects.put("APPID", Constants.APPID);
                    objects.put("COMPANYID", Constants.COMPANYID);
                    objects.put("FORMID",   Constants.FORM_ID_DEPT);
                    objects.put("TIMESTAMP", timestamp);
                    objects.put("DOCENTRY",docEntry);
                    String requestJsonAddArg = "{\n" +
                    "    \"U_OEPT\":[\n" +
                            "        {\n" +
                            "            \"DocEntry\":" + docEntry + ",\n" +
                            "            \"RowStatus\":\"U\"\n" +
                            "        }\n" +
                            "    ],\n" +
                            "    \"U_EPT1\":[\n" +
                            "        {\n" +
                            "            \"ParentName\":\"综合一组\",\n" +
                            "            \"DeptName\":\"综合销售部22\",\n" +
                            "            \"LineNum\":1,\n" +
                            "            \"RowStatus\":\"D\"\n" +
                            "        },\n" +
                            "    ]\n" +
                            "}";
                    String md5Token = SignUtil.makeMd5Token(objects, Constants.SECRETKEY, requestJsonAddArg);
                    url.append(Constants.DOMAIN_PORT).append(Constants.UPDATE)
                            .append("/").append(Constants.FORM_ID_DEPT)
                            .append("/").append(docEntry)
                            .append("/").append(timestamp)
                            .append("/").append(md5Token);
                    System.out.println("url:" + url);
                    try {
                        String s = HttpRequest.post(url.toString())
                                .body(requestJsonAddArg)
                                .execute()
                                .body();
                        System.out.println("r:" + s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // 关系映射表删除根据user_id
                    departmentMapper.deleteById(department.getId());
                }
            })
            .build();

    /**
     * 飞书订阅事件回调
     *
     * @param request
     * @param response
     * @throws Throwable
     */
    @RequestMapping(value = "/feishu/webhook/event")
    public void event(HttpServletRequest request, HttpServletResponse response)
            throws Throwable {
        servletAdapter.handleEvent(request, response, EVENT_DISPATCHER);
    }

    /**
     * 初始化部门映射
     *
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/init/insertDepartments")
    public String initDepartment() {
        // 如果SAP部门为空就向SAP系统插入所有飞书导出的部门列表
        // 获取飞书部门列表
        List<ExcelDept> excelDeptList = systemService.parseExcel();
        for (ExcelDept dept : excelDeptList) {
            JSONObject requestJson = new JSONObject();
            requestJson.put("DeptCode", dept.getId());
            requestJson.put("Name", dept.getName());
            requestJson.put("ParentCode", dept.getParentId());
            requestJson.put("U_MarketDept", "N");
            requestJson.put("RowStatus", "A");
            requestJson.put("DocEntry", 1);
            String requestJsonAddArg = "{\n" +
                    "    \"U_OEPT\":[\n" +
                    "        {\n" +
                    "            \"DocEntry\":1,\n" +
                    "            \"RowStatus\":\"A\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"U_EPT1\":[\n" +
                    "        {\n" +
                    "            \"ParentName\":\"" + dept.getParentId() + "\" ,\n" +
                    "            \"DeptName\":\"" + dept.getName() + "\",\n" +
                    "            \"LineNum\":1,\n" +
                    "            \"RowStatus\":\"A\",\n" +
                    "            \"DocEntry\":2\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";

            // 2.接口参数处理
            String timestamp = TimeUtil.getTimestamp();
            StringBuilder url = new StringBuilder();
            Map<String, String> objects = new HashMap<>();
            objects.put("APPID", Constants.APPID);
            objects.put("COMPANYID", Constants.COMPANYID);
            objects.put("TIMESTAMP", timestamp);
            objects.put("FORMID", Constants.FORM_ID_DEPT);
            String md5Token = SignUtil.makeMd5Token(objects, Constants.SECRETKEY, requestJsonAddArg);
            url.append(Constants.DOMAIN_PORT).append(Constants.ADD)
                    .append("/").append(Constants.FORM_ID_DEPT)
                    .append("/").append(timestamp).append("/").append(md5Token);
            // 3.调用SAP接口
            try {
                System.out.println("url: " + url);
                String resultStr = HttpRequest.post(url.toString())
                        .body(requestJsonAddArg)
                        .execute()
                        .body();
                if (StringUtils.isNotEmpty(resultStr)) {
                    JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
                    String resultCode = resultObject.getString("Code");
                    if (StringUtils.isNotEmpty(resultCode) && "0".equals(resultCode)) {
                        log.info("success: {}", resultStr);
                    } else {
                        log.info("fail: {}", resultStr);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "sucess";
    }

        // 根据部门名匹配
//        List<Department> departments = new ArrayList<>();
//        for (ExcelDept fs : newExcelDepts) {
//            for (SapDepartmentJson sap : sapDepts) {
//                // 系统运行前需保证两边系统的部门完全一致
//                if (sap.getName().equals(fs.getContent())) {
//                    Department department = new Department();
//                    department.setId(fs.getId());
//                    department.setParentId(fs.getParentID());
//                    department.setSapId(sap.getDeptCode());
//                    department.setSapParentId(sap.getParentCode());
//                    department.setName(sap.getName());
//                    departments.add(department);
//                    break;
//                }
//            }
//        }
//
//        // 根据部门名称匹配不上至少一个对应的部门
//        if (departments.size() == 0) {
//            for (ExcelDept fs : newExcelDepts) {
//                Department department = new Department();
//                department.setId(fs.getId());
//                department.setParentId(fs.getParentID());
//                department.setSapId(null);
//                department.setSapParentId(null);
//                department.setName(fs.getContent());
//                departments.add(department);
//            }
//        }
//        if (CollUtil.isNotEmpty(departments)) {
//            departmentMapper.insertBatch(departments);
//        }
//        return "success";
//    }

    @RequestMapping(value = "/initUser")
    @ResponseBody
    public String initUser() throws Exception {
        long number = userMapper.selectCount(null);
        System.out.println(number);

//        int number = userDao.count();
//        if (number > 0) {
//            return "No synchronization";
//        }
//
//        // 飞书用户列表查询
//        List<ExcelUser> excelUsers = new ArrayList<>();
//        ClassPathResource classPathResource = new ClassPathResource("通讯录-导出.xlsx");
//        EasyExcel.read(classPathResource.getInputStream(), ExcelUser.class, new AnalysisEventListener<ExcelUser>() {
//            @Override
//            public void invoke(ExcelUser user, AnalysisContext context) {
//                excelUsers.add(user);
//            }
//
//            @Override
//            public void doAfterAllAnalysed(AnalysisContext context) {
//            }
//        }).sheet().headRowNumber(3).doRead();
//
//        // 处理部门逗号，有则取逗号分割第一个
//        for (ExcelUser excelUser : excelUsers) {
//            if (excelUser.getDeptName().contains(",")) {
//                excelUser.setDeptName(excelUser.getDeptName().substring(0, excelUser.getDeptName().indexOf(",")));
//            }
//        }
//
//        // 处理飞书导入的部门名
//        for (ExcelUser excelUser : excelUsers) {
//            if (excelUser.getDeptName().contains("|")) {
//                excelUser.setDeptName(excelUser.getDeptName().substring(0, excelUser.getDeptName().indexOf("|")));
//            }
//            excelUser.setDeptName(excelUser.getDeptName().substring(excelUser.getDeptName().indexOf("/")));
//        }
//
//        // 去除多余层级
//        for (ExcelUser newExcelUser : excelUsers) {
//            String temp;
//            temp = newExcelUser.getDeptName().substring(newExcelUser.getDeptName().lastIndexOf("/") + 1);
//            newExcelUser.setDeptName(temp);
//        }
//
//        // 部门匹配
//        List<Department> departments = departmentMapper.selectAll();
//        List<User> users = new ArrayList<>();
//
//        for (ExcelUser excelUser : excelUsers) {
//            User user = new User();
////            user.setSapId(excelUser.getId());
//            user.setName(excelUser.getName());
////            user.setId(excelUser.getId());
//            user.setId(0L);
//            for (Department department : departments) {
//                if (department.getName().equals(excelUser.getDeptName())) {
////                    user.setDeptId(department.getId());
//                    break;
//                }
//            }
//            users.add(user);
//        }
//        // 添加到用户映射表
//        userDao.addList(users);

        return "success";
    }

}