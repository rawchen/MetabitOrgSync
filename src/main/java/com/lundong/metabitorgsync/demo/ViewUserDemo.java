package com.lundong.metabitorgsync.demo;

import com.kingdee.bos.webapi.sdk.K3CloudApi;

/**
 * @author RawChen
 * @date 2023-04-23 15:55
 */
public class ViewUserDemo {
    public static void main(String[] args) throws Exception {

//        String url = "http://192.168.121.129/k3cloud/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc";
//        String requestJson = "{\n" +
//                "    \"formid\": \"SEC_User\",\n" +
//                "    \"data\": \"{\\\"CreateOrgId\\\":0,\\\"Number\\\":\\\"\\\",\\\"Id\\\":\\\"16394\\\",\\\"IsSortBySeq\\\":\\\"false\\\"}\"\n" +
//                "}";
//        String s = HttpRequest.post(url)
//                .cookie(SignUtil.loginCookies())
//                .body(requestJson)
//                .execute()
//                .body();
//        System.out.println(s);

        K3CloudApi api = new K3CloudApi();
        String a = api.view("SEC_User", "{\"CreateOrgId\":0,\"Number\":\"\",\"Id\":\"16394\",\"IsSortBySeq\":\"false\"}");
    }

}
