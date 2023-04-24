package com.lundong.metabitorgsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-04-24 17:07
 */
@Data
public class KingdeeDept {

    @JSONField(name = "FNAME")
    private String name;

    @JSONField(name = "FDEPTID")
    private String deptId;

    @JSONField(name = "FPKID")
    private String pkId;
}
