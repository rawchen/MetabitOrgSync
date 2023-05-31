package com.lundong.metabitorgsync;

import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.lundong.metabitorgsync.config.Constants;
import com.lundong.metabitorgsync.entity.FeishuDept;
import com.lundong.metabitorgsync.entity.FeishuUser;
import com.lundong.metabitorgsync.entity.KingdeeDept;
import com.lundong.metabitorgsync.entity.KingdeeUser;
import com.lundong.metabitorgsync.service.DeptService;
import com.lundong.metabitorgsync.service.UserService;
import com.lundong.metabitorgsync.service.impl.DeptServiceImpl;
import com.lundong.metabitorgsync.service.impl.UserServiceImpl;
import com.lundong.metabitorgsync.util.SignUtil;
import com.lundong.metabitorgsync.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MetabitOrgSyncApplicationTests {

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

}
