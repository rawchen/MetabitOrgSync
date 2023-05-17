package com.lundong.metabitorgsync.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.lundong.metabitorgsync.entity.KingdeeOrgPost;
import com.lundong.metabitorgsync.service.OrgPostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author RawChen
 * @date 2023-05-17 17:13
 */
@Slf4j
@Service
public class OrgPostServiceImpl implements OrgPostService {

	/**
	 * 获取Kingdee岗位信息列表
	 *
	 * @return
	 */
	public List<KingdeeOrgPost> queryOrgPostList(String filterString) {
		List<KingdeeOrgPost> kingdeeOrgPostList = new ArrayList<>();
		K3CloudApi api = new K3CloudApi();
		try {
			String formJson = "{\"FormId\":\"HR_ORG_HRPOST\",\"FieldKeys\":\"FNAME,FNUMBER\",\"FilterString\":[],\"OrderString\":\"\",\"TopRowCount\":0,\"StartRow\":0,\"Limit\":2000,\"SubSystemId\":\"\"}";
			if (filterString != null && !"".equals(filterString)) {
				formJson = formJson.replaceAll("\\[\\]", "\"" + filterString + "\"");
			}
			String resultString = api.executeBillQueryJson(formJson);
			JSONArray resultArray = (JSONArray) JSONObject.parse(resultString);
			if (resultArray == null) {
				return Collections.emptyList();
			}
			for (int i = 0; i < resultArray.size(); i++) {
				JSONArray jsonArray = (JSONArray) resultArray.get(i);
				KingdeeOrgPost kingdeeOrgPost = new KingdeeOrgPost();
				kingdeeOrgPost.setName(jsonArray.getString(0));
				kingdeeOrgPost.setNumber(jsonArray.getString(1));
				kingdeeOrgPostList.add(kingdeeOrgPost);
			}
			return kingdeeOrgPostList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	/**
	 * 获取Kingdee岗位信息列表
	 *
	 * @return
	 */
	public List<KingdeeOrgPost> queryOrgPostList() {
		return queryOrgPostList(null);
	}
}