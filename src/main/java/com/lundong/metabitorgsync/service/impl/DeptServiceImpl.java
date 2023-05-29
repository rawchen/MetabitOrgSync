package com.lundong.metabitorgsync.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.lundong.metabitorgsync.entity.KingdeeDept;
import com.lundong.metabitorgsync.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author RawChen
 * @date 2023-03-08 19:52
 */
@Slf4j
@Service
public class DeptServiceImpl implements DeptService {

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
			System.out.println(resultString);
			JSONArray resultArray = (JSONArray) JSONObject.parse(resultString);
			if (resultArray == null) {
				return Collections.emptyList();
			}

			for (int i = 0; i < resultArray.size(); i++) {
				JSONArray jsonArray = (JSONArray) resultArray.get(i);
				System.out.println(jsonArray.toJSONString());
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
}