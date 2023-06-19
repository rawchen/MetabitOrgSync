package com.lundong.metabitorgsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-04-24 17:39
 */
@Data
public class KingdeePerson {

    @JSONField(name = "FName")
    private String name;

    @JSONField(name = "fNumber")
    private String number;

    @JSONField(name = "fEmpInfo")
    private String empInfo;
}
