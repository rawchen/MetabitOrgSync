package com.lundong.metabitorgsync.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lundong.metabitorgsync.config.Constants;
import com.lundong.metabitorgsync.entity.CorehrDepartment;
import com.lundong.metabitorgsync.entity.FeishuDept;
import com.lundong.metabitorgsync.entity.FeishuOffboarding;
import com.lundong.metabitorgsync.entity.FeishuUser;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author RawChen
 * @date 2023-03-08 18:37
 */
public class SignUtil {

	/**
	 * 飞书自建应用获取tenant_access_token
	 */
	public static String getAccessToken(String appId, String appSecret) {
		JSONObject object = new JSONObject();
		object.put("app_id", appId);
		object.put("app_secret", appSecret);
		String resultStr = HttpRequest.post("https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal")
				.form(object)
				.timeout(2000)
				.execute().body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if (!"0".equals(resultObject.getString("code"))) {
				return "";
			} else {
				String tenantAccessToken = resultObject.getString("tenant_access_token");
				if (tenantAccessToken != null) {
					return tenantAccessToken;
				}
			}
		}
		return "";
	}

	/**
	 * 查询雇佣信息
	 *
	 * @param accessToken
	 * @param employmentId
	 * @return
	 */
	public static String getHireInformation(String accessToken, String employmentId) {
		String resultStr = HttpRequest.get(
						"https://open.feishu.cn/open-apis/corehr/v1/employments/"
								+ employmentId
								+ "?department_id_type=people_corehr_department_id&user_id_type=people_corehr_id")
				.header("Authorization", "Bearer " + accessToken)
				.execute().body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if (!"0".equals(resultObject.getString("code"))) {
				return "";
			} else {
				JSONObject data = (JSONObject) resultObject.get("data");
				JSONObject department = (JSONObject) data.get("employment");
				String personId = department.getString("person_id");
				System.out.println("per " + personId);

				return personId;
			}
		}
		return "";
	}


	/**
	 * 查询个人信息
	 *
	 * @param accessToken
	 * @return
	 */
	public static String getPersons(String accessToken, String personId) {
		String resultStr = HttpRequest.get(
						"https://open.feishu.cn/open-apis/corehr/v1/persons/"
								+ personId
				)
				.header("Authorization", "Bearer " + accessToken)
				.execute().body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if (!"0".equals(resultObject.getString("code"))) {
				return "";
			} else {
				JSONObject data = (JSONObject) resultObject.get("data");
				JSONObject department = (JSONObject) data.get("person");
				String preferredName = department.getString("preferred_name");

				return preferredName;
			}
		}
		return "";
	}

	/**
	 * 根据OPEN ID获取部门ID和部门名
	 *
	 * @param accessToken
	 * @param openDepartmentId
	 * @return
	 */
	public static String getDepartmentIdAndName(String accessToken, String openDepartmentId) {
		String resultStr = HttpRequest.get(
						"https://open.feishu.cn/open-apis/contact/v3/departments/"
								+ openDepartmentId
								+ "?department_id_type=open_department_id&user_id_type=user_id")
				.header("Authorization", "Bearer " + accessToken)
				.execute().body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if (!"0".equals(resultObject.getString("code"))) {
				return "";
			} else {
				JSONObject data = (JSONObject) resultObject.get("data");
				JSONObject department = (JSONObject) data.get("department");
				String department_id = department.getString("department_id");
				String name = department.getString("name");
				return department_id + "," + name;
			}
		}
		return "";
	}

	/**
	 * 根据OPEN ID获取部门ID和部门名(已自动生成access token)
	 *
	 * @param openDepartmentId
	 * @return
	 */
	public static String getDepartmentIdAndName(String openDepartmentId) {
		if (StringUtil.isEmpty(openDepartmentId) || "0".equals(openDepartmentId)) {
			return "0";
		} else {
			String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
			return getDepartmentIdAndName(accessToken, openDepartmentId);
		}
	}

	/**
	 * 多维表格新增多条记录
	 *
	 * @param json
	 * @return
	 */
	public static List<String> batchInsertRecord(String json, String appToken, String tableId, String name) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return batchInsertRecord(accessToken, json, appToken, tableId, name);
	}

	/**
	 * 多维表格新增多条记录
	 *
	 * @param accessToken
	 * @param json
	 * @return
	 */
	private static List<String> batchInsertRecord(String accessToken, String json, String appToken, String tableId, String name) {
		List<String> recordIds = new ArrayList<>();
		String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/bitable/v1/apps/" + appToken + "/tables/" + tableId + "/records/batch_create")
				.header("Authorization", "Bearer " + accessToken)
				.body(json)
				.execute()
				.body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if (!"0".equals(resultObject.getString("code"))) {
				System.out.println("批量插入失败：" + resultObject.getString("msg"));
				return new ArrayList<>();
			} else {
				JSONObject data = (JSONObject) resultObject.get("data");
				JSONArray records = (JSONArray) data.get("records");
				for (int i = 0; i < records.size(); i++) {
					JSONObject jsonObject = records.getJSONObject(i);
					String recordId = jsonObject.getString("record_id");
					JSONObject fields = (JSONObject) jsonObject.get("fields");
					String nameTemp = fields.getString(name);
					if (nameTemp == null) {
						nameTemp = " ";
					}
					recordIds.add(recordId + "," + nameTemp);
				}
			}
		}
		return recordIds;
	}

	/**
	 * 批量根据内存记录的插入记录ids清空表格
	 *
	 * @param recordIds
	 * @param appToken
	 * @param tableId
	 */
	public static void batchClearTable(List<String> recordIds, String appToken, String tableId) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		batchClearTable(accessToken, recordIds, appToken, tableId);
	}

	/**
	 * 批量根据内存记录的插入记录ids清空表格
	 *
	 * @param accessToken
	 * @param recordIds
	 * @param appToken
	 * @param tableId
	 */
	public static void batchClearTable(String accessToken, List<String> recordIds, String appToken, String tableId) {
		System.out.println("===开始批量删除 " + tableId + "===");
		if (recordIds != null && recordIds.size() > 0) {
			List<List<String>> partitions = ListUtils.partition(recordIds, 500);
			for (List<String> partition : partitions) {
				JSONObject object = new JSONObject();
				object.put("records", JSONArray.parseArray(JSON.toJSONString(partition)));
				String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/bitable/v1/apps/" + appToken + "/tables/" + tableId + "/records/batch_delete")
						.header("Authorization", "Bearer " + accessToken)
						.body(object.toJSONString())
						.execute()
						.body();
				if (StringUtils.isNotEmpty(resultStr)) {
					JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
					if (!"0".equals(resultObject.getString("code"))) {
						System.out.println("批量删除失败：" + resultObject.getString("msg"));
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("===批量删除完成===");
	}

	/**
	 * 获取部门直属用户列表
	 *
	 * @return
	 */
	public static List<FeishuUser> findByDepartment() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return findByDepartment(accessToken);
	}

	/**
	 * 获取部门直属用户列表
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<FeishuUser> findByDepartment(String accessToken) {
		List<FeishuUser> users = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		while (true) {
			param.put("department_id", 0);
			param.put("page_size", 50);
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/contact/v3/users/find_by_department")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
//			System.out.println(resultStr);
			JSONObject jsonObject = JSON.parseObject(resultStr);
			JSONObject data = (JSONObject) jsonObject.get("data");
			JSONArray items = (JSONArray) data.get("items");
			for (int i = 0; i < items.size(); i++) {
				// 构造飞书用户对象
				FeishuUser feishuUser = items.getJSONObject(i).toJavaObject(FeishuUser.class);
				JSONArray departmentIds = items.getJSONObject(i).getJSONArray("department_ids");
				if (departmentIds.size() >= 2) {
					feishuUser.setDepartmentId(departmentIds.getString(1));
				} else {
					feishuUser.setDepartmentId("0");
				}
				users.add(feishuUser);
			}

			if ((boolean) data.get("has_more")) {
				param.put("page_token", data.getString("page_token"));
			} else {
				break;
			}
		}
		return users;
	}

	/**
	 * 获取子部门列表
	 *
	 * @return
	 */
	public static List<FeishuDept> departments() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return departments(accessToken);
	}

	/**
	 * 获取子部门列表
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<FeishuDept> departments(String accessToken) {
		List<FeishuDept> depts = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		while (true) {
			param.put("user_id_type", "open_id");
			param.put("department_id_type", "department_id");
			param.put("fetch_child", true);
			param.put("page_size", 10);
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/contact/v3/departments/0/children")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
//			System.out.println(resultStr);
			JSONObject jsonObject = JSON.parseObject(resultStr);
			JSONObject data = (JSONObject) jsonObject.get("data");
			JSONArray items = (JSONArray) data.get("items");
			for (int i = 0; i < items.size(); i++) {
				// 构造飞书用户对象
				FeishuDept feishuDept = items.getJSONObject(i).toJavaObject(FeishuDept.class);
				depts.add(feishuDept);
			}

			if ((boolean) data.get("has_more")) {
				param.put("page_token", data.getString("page_token"));
			} else {
				break;
			}
		}
		return depts;
	}

	/**
	 * 登录金蝶测试
	 *
	 * @return
	 */
	public static List<HttpCookie> loginCookies() {
		String loginUrl = "http://192.168.121.129/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
		String loginJson = "{\n" +
				"    \"acctID\": \"642427270e9f87\",\n" +
				"    \"username\": \"Administrator\",\n" +
				"    \"password\": \"Admin123456.\",\n" +
				"    \"lcid\": \"2052\"\n" +
				"}";
		HttpResponse loginResponse = HttpRequest.post(loginUrl.toString())
				.body(loginJson)
				.timeout(2000)
				.execute();
		return loginResponse.getCookies();
	}

	/**
	 * 飞书人事（标准版）花名册
	 *
	 * @return
	 */
	public static List<FeishuUser> findEmployees() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return findEmployees(accessToken);
	}

	/**
	 * 飞书人事（标准版）花名册
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<FeishuUser> findEmployees(String accessToken) {
		List<FeishuUser> users = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		while (true) {
			param.put("view", "full");
			param.put("user_id_type", "user_id");
			param.put("page_size", 100);
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/ehr/v1/employees")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
			JSONObject jsonObject = JSON.parseObject(resultStr);
			JSONObject data = (JSONObject) jsonObject.get("data");
			JSONArray items = (JSONArray) data.get("items");
			for (int i = 0; i < items.size(); i++) {
				// 构造飞书用户对象
				FeishuUser feishuUser = items.getJSONObject(i).getJSONObject("system_fields").toJavaObject(FeishuUser.class);
				feishuUser.setUserId(items.getJSONObject(i).getString("user_id"));
				if (items.getJSONObject(i).getJSONObject("system_fields").getJSONObject("job") != null) {
					feishuUser.setJobTitle(items.getJSONObject(i).getJSONObject("system_fields").getJSONObject("job").getString("name"));
				}
				users.add(feishuUser);
			}
			if ((boolean) data.get("has_more")) {
				param.put("page_token", data.getString("page_token"));
			} else {
				break;
			}
		}
		return users;
	}

	/**
	 * 飞书人事（企业版）查询单个部门编码
	 *
	 * @param accessToken
	 * @return
	 */
	public static String corehrDepartment(String accessToken, String departmentId) {
		String code = "";
		String resultStr = HttpRequest.get(
						"https://open.feishu.cn/open-apis/corehr/v1/departments/"
								+ departmentId
								+ "?department_id_type=department_id&user_id_type=user_id")
				.timeout(2000)
				.header("Authorization", "Bearer " + accessToken)
				.execute()
				.body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if ("0".equals(resultObject.getString("code"))) {
				JSONObject data = resultObject.getJSONObject("data");
				JSONObject department = data.getJSONObject("department");
				if (department != null) {
					JSONObject hiberarchyCommon = department.getJSONObject("hiberarchy_common");
					if (hiberarchyCommon != null) {
						String codeTemp = hiberarchyCommon.getString("code");
						if (codeTemp != null) {
							return codeTemp;
						}
					}
				}
			}
		}
		return code;
	}

	/**
	 * 飞书人事（企业版）查询单个部门编码
	 *
	 * @return
	 */
	public static String corehrDepartment(String departmentId) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return corehrDepartment(accessToken, departmentId);
	}

	/**
	 * 飞书人事（企业版）查询单个部门数据
	 *
	 * @return
	 */
	public static String corehrDepartmentData(String departmentId) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		String resultStr = HttpRequest.get(
						"https://open.feishu.cn/open-apis/corehr/v1/departments/"
								+ departmentId
								+ "?department_id_type=department_id&user_id_type=user_id")
				.timeout(2000)
				.header("Authorization", "Bearer " + accessToken)
				.execute()
				.body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if (!"0".equals(resultObject.getString("code"))) {
				return "";
			} else {
				return resultStr;
			}
		}
		return "";
	}

	/**
	 * 调用搜索离职信息
	 *
	 * @param employmentId
	 */
	public static boolean corehrOffboardingsSearch(String accessToken, String employmentId) {
		List<FeishuOffboarding> offboardingList = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		String pageToken = "";
		while (true) {
			param.put("page_size", 100);
			param.put("user_id_type", "people_corehr_id");

//			JSONObject object = new JSONObject();
//			object.put("employment_ids", new JSONArray().add(employmentId));
			String resultStr = HttpRequest.post("https://open.feishu.cn/open-apis/corehr/v1/offboardings/search?page_size=100&user_id_type=people_corehr_id&pageToken=" + pageToken)
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.body("{\"employment_ids\":[\"" + employmentId + "\"]}")
					.execute()
					.body();
			JSONObject jsonObject = JSON.parseObject(resultStr);
			if (jsonObject.getInteger("code") != 0) {
				return false;
			}
			JSONObject data = (JSONObject) jsonObject.get("data");
			JSONArray items = (JSONArray) data.get("items");
			if (items != null && items.size() > 0) {
				for (int i = 0; i < items.size(); i++) {
					// 构造数据对象
					FeishuOffboarding offboarding = new FeishuOffboarding();
					offboarding.setProcessId(items.getJSONObject(i).getJSONObject("application_info").getString("process_id"));
					offboarding.setApplyInitiatorId(items.getJSONObject(i).getJSONObject("application_info").getString("apply_initiator_id"));
					offboarding.setChecklistStatus(items.getJSONObject(i).getJSONObject("offboarding_checklist").getString("checklist_status"));
					offboardingList.add(offboarding);
				}
				if ((boolean) data.get("has_more")) {
					pageToken = data.getString("page_token");
//					param.put("page_token", data.getString("page_token"));
				} else {
					break;
				}
			}

		}

		for (FeishuOffboarding offboarding : offboardingList) {
			if ("Finished".equals(offboarding.getChecklistStatus())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 调用搜索离职信息
	 *
	 * @param employmentId
	 */
	public static boolean corehrOffboardingsSearch(String employmentId) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return corehrOffboardingsSearch(accessToken, employmentId);
	}

	/**
	 * 批量查询部门，只获取是否启用和部门ID
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<CorehrDepartment> findCorehrDepartmentIsActive(String accessToken) {
		List<CorehrDepartment> departments = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		while (true) {
			param.put("department_id_type", "department_id");
			param.put("user_id_type", "user_id");
			param.put("page_size", 100);
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/corehr/v1/departments")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
			JSONObject jsonObject = JSON.parseObject(resultStr);
			JSONObject data = (JSONObject) jsonObject.get("data");
			JSONArray items = (JSONArray) data.get("items");
			for (int i = 0; i < items.size(); i++) {
				// 构造飞书用户对象
				CorehrDepartment department = new CorehrDepartment();
				department.setActive(items.getJSONObject(i).getJSONObject("hiberarchy_common").getBoolean("active"));
				department.setId(items.getJSONObject(i).getString("id"));
				departments.add(department);
			}
			if ((boolean) data.get("has_more")) {
				param.put("page_token", data.getString("page_token"));
			} else {
				break;
			}
		}
		// 过滤掉id为null的
		departments = departments.stream().filter(d -> d.getId() != null && !"".equals(d.getId())).collect(Collectors.toList());
		return departments;
	}

	/**
	 * 批量查询部门，只获取是否启用和部门ID
	 *
	 * @return
	 */
	public static List<CorehrDepartment> findCorehrDepartmentIsActive() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return findCorehrDepartmentIsActive(accessToken);
	}
}
