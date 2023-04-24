package com.lundong.metabitorgsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-04-24 17:39
 */
@Data
public class KingdeeUser {

    @JSONField(name = "FPKID")
    private String pkId;

    @JSONField(name = "FUSERID")
    private String userId;

    @JSONField(name = "FNAME")
    private String name;

    @JSONField(name = "FLOCALEID")
    private String localeId;

    @JSONField(name = "FPRIMARYGROUP")
    private String primaryGroup;
}
