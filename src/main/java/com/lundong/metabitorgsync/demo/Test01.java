package com.lundong.metabitorgsync.demo;

import com.lundong.metabitorgsync.entity.FeishuUser;
import com.lundong.metabitorgsync.util.SignUtil;

import java.util.List;

/**
 * @author RawChen
 * @date 2023-04-24 13:53
 */
public class Test01 {
    public static void main(String[] args) {
        List<FeishuUser> users = SignUtil.findByDepartment();
        for (FeishuUser s : users) {
            System.out.println(s);
            String deptIdAndName = SignUtil.getDepartmentIdAndName(s.getDepartmentId());
            System.out.println(deptIdAndName);
            String deptId = "";
            String deptName = "";
            if (deptIdAndName.contains(",")
                    && deptIdAndName.split(",")[0] != null
                    && deptIdAndName.split(",")[1] != null) {
                String[] split = deptIdAndName.split(",");
                deptId = split[0];
                deptName = split[1];
            }
            s.setDepartmentId(deptId);
            s.setDeptName(deptName);

        }


        for (FeishuUser s : users) {
            System.out.println(s);
        }
    }
}
