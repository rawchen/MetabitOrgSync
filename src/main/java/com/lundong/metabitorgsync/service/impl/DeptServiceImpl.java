package com.lundong.metabitorgsync.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.lundong.metabitorgsync.entity.CorehrDepartment;
import com.lundong.metabitorgsync.entity.Department;
import com.lundong.metabitorgsync.entity.KingdeeDept;
import com.lundong.metabitorgsync.mapper.DepartmentMapper;
import com.lundong.metabitorgsync.service.DeptService;
import com.lundong.metabitorgsync.util.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RawChen
 * @date 2023-03-08 19:52
 */
@Slf4j
@Service
public class DeptServiceImpl implements DeptService {

	@Autowired
	private DepartmentMapper departmentMapper;

	K3CloudApi api = new K3CloudApi();

	/**
	 * 获取Kingdee部门列表
	 *
	 * @return
	 */
	public List<KingdeeDept> queryDepartmentList() {
		List<KingdeeDept> kingdeeDeptList = new ArrayList<>();
		try {
			String resultString = api.executeBillQueryJson("{\"FormId\":\"BD_Department\",\"FieldKeys\":\"FNAME,FDEPTID,FNUMBER,FPARENTID\",\"FilterString\":[],\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}");
//			System.out.println(resultString);
			JSONArray resultArray = (JSONArray) JSONObject.parse(resultString);
			if (resultArray == null) {
				return Collections.emptyList();
			}

			for (int i = 0; i < resultArray.size(); i++) {
				JSONArray jsonArray = (JSONArray) resultArray.get(i);
//				System.out.println(jsonArray.toJSONString());
				KingdeeDept kingdeeDept = new KingdeeDept();
				kingdeeDept.setName(jsonArray.getString(0));
				kingdeeDept.setDeptId(jsonArray.getString(1));
				kingdeeDept.setNumber(jsonArray.getString(2));
				kingdeeDept.setParentId(jsonArray.getString(3));
				kingdeeDeptList.add(kingdeeDept);
			}
			return kingdeeDeptList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	/**
	 * 修改Kingdee部门code
	 *
	 * @return
	 */
	public boolean updateDepartment(String kingdeeDeptId, String code) {
		String deptSaveJson = "{\"Model\":{\"FDEPTID\":\"部门ID\"," +
				"\"FHelpCode\":\"助记码\"}";
		deptSaveJson = deptSaveJson.replaceAll("部门ID", kingdeeDeptId);
		deptSaveJson = deptSaveJson.replaceAll("助记码", code);
		String deptSaveResult = null;
		try {
			deptSaveResult = api.save("BD_Department", deptSaveJson);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		JSONObject postObject = JSONObject.parseObject(deptSaveResult);
		JSONObject resultObject = (JSONObject) postObject.get("Result");
		JSONObject responseStatus = (JSONObject) resultObject.get("ResponseStatus");
		return responseStatus.getBoolean("IsSuccess");
	}

	/**
	 * 每天定时检查所有停用部门
	 */
//	@Scheduled(cron = "0 0 1 ? * *")
	public void syncStopDeptData() {

		// 查询所有部门，过滤出active为false
		List<CorehrDepartment> corehrDepartmentIsActive = SignUtil.findCorehrDepartmentIsActive();
		corehrDepartmentIsActive = corehrDepartmentIsActive.stream().filter(d -> !d.getActive()).collect(Collectors.toList());
		log.info("Scheduled isActive: {}", corehrDepartmentIsActive.size());

		for (CorehrDepartment corehrDepartment : corehrDepartmentIsActive) {

			Department department = departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getFeishuDeptId, corehrDepartment.getId()).last("limit 1"));
			if (department == null) {
				log.info("P2DepartmentDeletedV3事件：用户映射表中根据department_id获取不到记录: {}", corehrDepartment.getId());
				return;
			}
			try {
				//
				String text = "{\"FormId\":\"HR_ORG_HRPOST\",\"FieldKeys\":\"FPOSTID\",\"FilterString\":\"FDept='" + department.getKingdeeDeptId() + "'\",\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}";
				final K3CloudApi api = new K3CloudApi();
				String unAuditEmpinfoResult = api.executeBillQueryJson(text);
				JSONArray jsonArray = JSONArray.parseArray(unAuditEmpinfoResult);
				//部门下的岗位
				for (int i = 0; i < jsonArray.size(); i++) {
					Object FPost = jsonArray.get(i);
					String stationReplace = FPost.toString().replace("[", "").replace("]", "");
					String empinfo = "{\"FormId\":\"BD_Empinfo\",\"FieldKeys\":\"FID\",\"FilterString\":\"FPost='" + stationReplace + "'\",\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}";
					String json = api.executeBillQueryJson(empinfo);
					//用户
					JSONArray userJson = JSONArray.parseArray(json);
					//岗位任职人员
					for (int n = 0; n < userJson.size(); n++) {
						String userReplace = userJson.get(n).toString().replace("[", "").replace("]", "");
						//反审核用户
						String bdEmpinfo = api.unAudit("BD_Empinfo", "{\"Ids\":\"" + userReplace + "\"}");
						//禁用用户
						String forbidEmpinfoResult = api.excuteOperation("BD_Empinfo", "Forbid", "{\"Ids\":\"" + userReplace + "\"}");
					}
					//反审核岗位
					String unAuditempinfo = api.unAudit("HR_ORG_HRPOST", "{\"Ids\":\"" + stationReplace + "\"}");
					//禁用岗位
					String unAuditStationResult = api.excuteOperation("HR_ORG_HRPOST", "Forbid", "{\"Ids\":\"" + stationReplace + "\"}");
				}
				//反审核部门
				String unDepartment = api.unAudit("BD_Department", "{\"Ids\":\"" + department.getKingdeeDeptId() + "\"}");
				JSONObject unAuditEmpinfoObject = JSONObject.parseObject(unDepartment);
				JSONObject unAuditResultObject = (JSONObject) unAuditEmpinfoObject.get("Result");
				JSONObject unAuditResponseStatus = (JSONObject) unAuditResultObject.get("ResponseStatus");
				//禁用部门
				String unAuditDepartmentResult = api.excuteOperation("BD_Department", "Forbid", "{\"Ids\":\"" + department.getKingdeeDeptId() + "\"}");
				JSONObject forbidEmpinfoObject = JSONObject.parseObject(unAuditDepartmentResult);
				JSONObject forbidResultObject = (JSONObject) forbidEmpinfoObject.get("Result");
				JSONObject forbidResponseStatus = (JSONObject) forbidResultObject.get("ResponseStatus");
				// 金蝶删除成功，映射表删除部门
				if (forbidResponseStatus.getBoolean("IsSuccess")) {
					departmentMapper.deleteById(department.getId());
					log.info("deleteDept success: {}", forbidResponseStatus);
				} else {
					log.info("deleteDept fail: {}", forbidResponseStatus.getJSONArray("Errors").toJSONString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}