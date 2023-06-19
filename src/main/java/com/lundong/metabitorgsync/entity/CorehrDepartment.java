package com.lundong.metabitorgsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-06-19 11:37
 */
@Data
public class CorehrDepartment {
	@JSONField(name = "id")
	private String id;

	@JSONField(name = "active")
	private Boolean active;
}
