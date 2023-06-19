package com.lundong.metabitorgsync;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.lundong.metabitorgsync.config.Constants;
import com.lundong.metabitorgsync.entity.*;
import com.lundong.metabitorgsync.mapper.UserMapper;
import com.lundong.metabitorgsync.service.DeptService;
import com.lundong.metabitorgsync.service.UserService;
import com.lundong.metabitorgsync.service.impl.DeptServiceImpl;
import com.lundong.metabitorgsync.service.impl.UserServiceImpl;
import com.lundong.metabitorgsync.util.SignUtil;
import com.lundong.metabitorgsync.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MetabitOrgSyncApplicationTests {

	@Autowired
	UserMapper userMapper;

	@Autowired
	UserService userService;

	@Test
	void contextLoads() {
		String s = TimeUtil.timestampToUTC("1677813905");
		System.out.println(s);
	}

	@Test
	void getParentId() {
		String accessToken = SignUtil.getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);

		String result = SignUtil.getDepartmentIdAndName(accessToken, "od-b66de0fbb2edb71b7f5b020d675a3e04");
		System.out.println(result);

		UserService userService = new UserServiceImpl();
		List<KingdeeUser> kingdeeUsers = userService.queryUserList();
		for (KingdeeUser kingdeeUser : kingdeeUsers) {
			System.out.println(kingdeeUser);
		}
	}

	@Test
	void saveOrgPost() throws Exception {
		K3CloudApi api = new K3CloudApi();
		String saveOrgPostData = "{\"NeedUpDateFields\":[],\"NeedReturnFields\":[]," +
				"\"IsDeleteEntry\":\"true\",\"SubSystemId\":\"\",\"IsVerifyBaseDataField\":\"false\"," +
				"\"IsEntryBatchFill\":\"true\",\"ValidateFlag\":\"true\",\"NumberSearch\":\"true\"," +
				"\"IsAutoAdjustField\":\"false\",\"InterationFlags\":\"\",\"IgnoreInterationFlag\":\"\"," +
				"\"IsControlPrecision\":\"false\",\"ValidateRepeatJson\":\"false\"," +
				"\"Model\":{\"FPOSTID\":0,\"FCreateOrgId\":{\"FNumber\":\"创建组织\"},\"FNumber\":\"\"," +
				"\"FUseOrgId\":{\"FNumber\":\"使用组织\"},\"FName\":\"名称\",\"FHelpCode\":\"\"," +
				"\"FDept\":{\"FNumber\":\"所属部门\"},\"FEffectDate\":\"1900-01-01\"," +
				"\"FLapseDate\":\"1900-01-01\",\"FDESCRIPTIONS\":\"\",\"FHRPostSubHead\":{\"FHRPOSTID\":0," +
				"\"FLEADERPOST\":\"false\"},\"FSHRMapEntity\":{\"FMAPID\":0}," +
				"\"FSubReportEntity\":[{\"FSubNumber\":\"\"}]}}";
		saveOrgPostData = saveOrgPostData.replaceAll("所属部门", "BM000022");
		saveOrgPostData = saveOrgPostData.replaceAll("名称", "测测测岗位");
		saveOrgPostData = saveOrgPostData.replaceAll("创建组织", "100");
		saveOrgPostData = saveOrgPostData.replaceAll("使用组织", "100");
		String saveOrgPostDataResult = api.save("HR_ORG_HRPOST", saveOrgPostData);
		System.out.println(saveOrgPostDataResult);
	}

	@Test
	void findEmployees() {
		String accessToken = SignUtil.getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);

		List<FeishuUser> employees = SignUtil.findEmployees(accessToken);
		for (FeishuUser employee : employees) {
			System.out.println(employee);
		}
	}

	@Test
	void getAccessToken() {
		String accessToken = SignUtil.getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		System.out.println(accessToken);
	}

	@Test
	void updateDepartment() throws Exception {
		K3CloudApi api = new K3CloudApi();
		String deptSaveJson = "{\"Model\":{\"FDEPTID\":\"部门ID\",\"FCreateOrgId\":{\"Number\":\"创建组织\"}," +
				"\"FNumber\":\"编号\",\"FUseOrgId\":{\"Number\":\"使用组织\"}," +
				"\"FHelpCode\":\"\"," +
				"\"FDescription\":\"\"," +
				"\"FDeptProperty\":{\"FNumber\":\"\"},\"FSHRMapEntity\":{\"FMAPID\":0}}}";
		deptSaveJson = deptSaveJson.replaceAll("创建组织", Constants.ORG_NUMBER);
		deptSaveJson = deptSaveJson.replaceAll("使用组织", Constants.ORG_NUMBER);
		deptSaveJson = deptSaveJson.replaceAll("编号", "BM000036");
		deptSaveJson = deptSaveJson.replaceAll("部门ID", "213105");
		String deptSaveResult = api.save("BD_Department", deptSaveJson);
		System.out.println(deptSaveResult);
	}

	@Test
	void testQueryDepartmentList() {
		DeptService deptService = new DeptServiceImpl();
		List<KingdeeDept> kingdeeDepts = deptService.queryDepartmentList();
		for (KingdeeDept kingdeeDept : kingdeeDepts) {
			System.out.println(kingdeeDept);
		}
	}

	@Test
	void testgetFeishuDepartmentList() {
		List<FeishuDept> depts = SignUtil.departments();
		for (FeishuDept dept : depts) {
			System.out.println(dept);
		}
	}

	@Test
	void testCorehrDepartmentData() {
		System.out.println(SignUtil.corehrDepartmentData("7199920317373005852"));
	}

	@Test
	void testCorehrDepartment() {
		System.out.println(SignUtil.corehrDepartment("7199920317373005852"));
	}

	@Test
	void testgetHireInformation() throws Exception {
		String accessToken = SignUtil.getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		String hireInformation = SignUtil.getHireInformation(accessToken, "7189444941343573562");
		String persons = SignUtil.getPersons(accessToken, hireInformation);
		User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
				.eq(User::getName, persons));
		System.out.println("user " +  user);
		// 改为如果是审核了就反审核，并且禁用
		K3CloudApi api = new K3CloudApi();
		String unAuditEmpinfoResult = api.unAudit("BD_Empinfo", "{\"Ids\":\"" + user.getFid() + "\"}");
		JSONObject unAuditEmpinfoObject = JSONObject.parseObject(unAuditEmpinfoResult);
		JSONObject unAuditResultObject = (JSONObject) unAuditEmpinfoObject.get("Result");
		JSONObject unAuditResponseStatus = (JSONObject) unAuditResultObject.get("ResponseStatus");

		String forbidEmpinfoResult = api.excuteOperation("BD_Empinfo", "Forbid", "{\"Ids\":\"" + user.getFid() + "\"}");
		JSONObject forbidEmpinfoObject = JSONObject.parseObject(forbidEmpinfoResult);
		JSONObject forbidResultObject = (JSONObject) forbidEmpinfoObject.get("Result");
		JSONObject forbidResponseStatus = (JSONObject) forbidResultObject.get("ResponseStatus");
	}

	@Test
	void testget() throws Exception {
		//通过部门id查询部门信息
		String deptDeleteJson = "{\"data\":\"{\"FormId\":\"HR_ORG_HRPOST\",\"FieldKeys\":\"FPOSTID\",\"FilterString\":\"FDept='" + 511778 + "'\",\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}\"}";
		String text = "{\"FormId\":\"HR_ORG_HRPOST\",\"FieldKeys\":\"FPOSTID\",\"FilterString\":\"FDept='" + "511778" + "'\",\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}";
		final  K3CloudApi api = new K3CloudApi();
		String unAuditEmpinfoResult = api.executeBillQueryJson(text);
		JSONArray jsonArray = JSONArray.parseArray(unAuditEmpinfoResult);
		for (int i = 0 ; i<jsonArray.size();i++ ){
			Object FPost = jsonArray.get(i);
			String stationReplace = FPost.toString().replace("[", "").replace("]", "");
			String empinfo = "{\"FormId\":\"BD_Empinfo\",\"FieldKeys\":\"FID\",\"FilterString\":\"FPost='" + stationReplace + "'\",\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}";
			String json = api.executeBillQueryJson(empinfo);
			//用户
			JSONArray userJson = JSONArray.parseArray(json);
			for (int n = 0 ; n<userJson.size();n++ ){
				String userReplace = userJson.get(n).toString().replace("[", "").replace("]", "");
				//反审核用户
				String bd_empinfo = api.unAudit("BD_Empinfo", "{\"Ids\":\"" + userReplace + "\"}");
				//禁用部门
				String forbidEmpinfoResult = api.excuteOperation("BD_Empinfo", "Forbid", "{\"Ids\":\"" + userReplace + "\"}");
			}
			//反审核部门
			String unAuditempinfo = api.unAudit("HR_ORG_HRPOST", "{\"Ids\":\"" + stationReplace + "\"}");
			//禁用部门
			String unAuditStationResult = api.excuteOperation("HR_ORG_HRPOST", "Forbid", "{\"Ids\":\"" + stationReplace + "\"}");
//			String json = api.executeBillQueryJson(empinfo);
		}
		//反审核部门
		String unDepartment = api.unAudit("BD_Department", "{\"Ids\":\"" + 511778 + "\"}");
		//禁用部门
		String unAuditDepartmentResult = api.excuteOperation("BD_Department", "Forbid", "{\"Ids\":\"" + 511778 + "\"}");
	}

	@Test
	void testCorehrOffboardingsSearch() {
		boolean r = SignUtil.corehrOffboardingsSearch("7189098249222145591");
		System.out.println(r);
	}

	@Test
	void testqueryPersonList() {
		List<KingdeePerson> personList = userService.queryPersonList("fEmpInfo='" + "448554" + "'");
		for (KingdeePerson kingdeePerson : personList) {
			System.out.println(kingdeePerson);
		}
	}

	@Test
	void testViewEmpInfo() throws Exception {
		final K3CloudApi api = new K3CloudApi();
		String viewStaffInfoJson = "{\"CreateOrgId\":0,\"Number\":\"\",\"Id\":\"448692\",\"IsSortBySeq\":\"false\"}";
		String bdEmpinfo = api.view("BD_Empinfo", viewStaffInfoJson);
		System.out.println(bdEmpinfo);
	}

	@Test
	void testFindCorehrDepartmentIsActive() {
		List<CorehrDepartment> corehrDepartmentIsActive = SignUtil.findCorehrDepartmentIsActive();
		for (CorehrDepartment department : corehrDepartmentIsActive) {
			System.out.println(department);
		}
		System.out.println(corehrDepartmentIsActive.size());
	}

}
