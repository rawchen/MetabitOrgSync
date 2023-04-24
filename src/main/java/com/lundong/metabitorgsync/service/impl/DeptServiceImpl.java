package com.lundong.metabitorgsync.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.lundong.metabitorgsync.entity.KingdeeDept;
import com.lundong.metabitorgsync.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @author RawChen
 * @date 2023-03-08 19:52
 */
@Slf4j
@Service
public class DeptServiceImpl implements DeptService {

	/**
	 * 获取Kingdee部门列表
	 *
	 * @return
	 */
	public List<KingdeeDept> queryDepartmentList() {
		K3CloudApi api = new K3CloudApi();
		try {
			String resultString = api.executeBillQueryJson("{\"FormId\":\"BD_Department\",\"FieldKeys\":\"FNAME,FDEPTID,FUSEORGID\",\"FilterString\":[],\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}");
			JSONObject result = (JSONObject) JSONObject.parse(resultString);
			JSONArray resultArray = (JSONArray) result.get("Result");
			if (resultArray == null) {
				return Collections.emptyList();
			}
			return JSONObject.parseArray(resultArray.toJSONString(), KingdeeDept.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}
}