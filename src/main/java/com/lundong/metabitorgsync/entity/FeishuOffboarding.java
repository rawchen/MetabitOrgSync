package com.lundong.metabitorgsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-06-09 18:27
 */
@Data
public class FeishuOffboarding {

	@JSONField(name = "process_id")
	private String processId;

	@JSONField(name = "checklist_status")
	private String checklistStatus;

	@JSONField(name = "apply_initiator_id")
	private String applyInitiatorId;

}
