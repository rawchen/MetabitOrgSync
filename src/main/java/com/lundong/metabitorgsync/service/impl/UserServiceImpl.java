package com.lundong.metabitorgsync.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.lundong.metabitorgsync.entity.KingdeeUser;
import com.lundong.metabitorgsync.service.UserService;
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
public class UserServiceImpl implements UserService {

	/**
	 * 获取Kingdee用户列表
	 *
	 * @return
	 */
	public List<KingdeeUser> queryUserList() {
		K3CloudApi api = new K3CloudApi();
		try {
			String resultString = api.executeBillQueryJson("{\"FormId\":\"SEC_User\",\"FieldKeys\":\"FUSERID,FNAME,FPRIMARYGROUP,FDESCRIPTION\",\"FilterString\":[],\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}");
			JSONObject result = (JSONObject) JSONObject.parse(resultString);
			JSONArray resultArray = (JSONArray) result.get("Result");
			if (resultArray == null) {
				return Collections.emptyList();
			}
			return JSONObject.parseArray(resultArray.toJSONString(), KingdeeUser.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}
}
