package com.lundong.metabitorgsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-04-24 17:39
 */
@Data
public class KingdeeUser {

    @JSONField(name = "FSTAFFID")
    private String staffId;

    @JSONField(name = "FNAME")
    private String name;

    @JSONField(name = "FSTAFFNUMBER")
    private String number;

    @JSONField(name = "FPostDept")
    private String kingdeeDeptId;

    @JSONField(name = "FID")
    private String fid;
}
