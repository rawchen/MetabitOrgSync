package com.lundong.metabitorgsync.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.lundong.metabitorgsync.entity.KingdeeUser;
import com.lundong.metabitorgsync.service.UserService;
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
public class UserServiceImpl implements UserService {

	/**
	 * 获取Kingdee用户列表
	 *
	 * @return
	 */
	public List<KingdeeUser> queryUserList() {
		List<KingdeeUser> kingdeeUserList = new ArrayList<>();
		K3CloudApi api = new K3CloudApi();
		try {
			String resultString = api.executeBillQueryJson("{\"FormId\":\"BD_Empinfo\",\"FieldKeys\":\"FSTAFFID,FNAME,FSTAFFNUMBER,FPostDept,FID\",\"FilterString\":[],\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}");
			JSONArray resultArray = (JSONArray) JSONObject.parse(resultString);
			if (resultArray == null) {
				return Collections.emptyList();
			}

			for (int i = 0; i < resultArray.size(); i++) {
				JSONArray jsonArray = (JSONArray) resultArray.get(i);
				KingdeeUser kingdeeUser = new KingdeeUser();
				kingdeeUser.setStaffId(jsonArray.getString(0));
				kingdeeUser.setName(jsonArray.getString(1));
				kingdeeUser.setNumber(jsonArray.getString(2));
				kingdeeUser.setKingdeeDeptId(jsonArray.getString(3));
				kingdeeUser.setFid(jsonArray.getString(4));
				kingdeeUserList.add(kingdeeUser);
			}
			return kingdeeUserList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}
}
