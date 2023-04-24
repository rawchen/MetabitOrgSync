package com.lundong.metabitorgsync.demo;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lundong.metabitorgsync.config.Constants;
import com.lundong.metabitorgsync.entity.FeishuDept;
import com.lundong.metabitorgsync.util.SignUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RawChen
 * @date 2023-04-24 16:48
 */
public class Test02 {
    public static void main(String[] args) {
        String accessToken = SignUtil.getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
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
        for (FeishuDept dept : depts) {
            System.out.println(dept);
        }
    }
}
