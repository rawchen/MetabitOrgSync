package com.lundong.metabitorgsync.event;

import com.google.gson.annotations.SerializedName;

/**
 * @author RawChen
 * @date 2023-06-09 15:36
 */
public class TargetUserId {

	@SerializedName("union_id")
	private String unionId;

	@SerializedName("user_id")
	private String userId;

	@SerializedName("open_id")
	private String openId;

	public String getUnionId() {
		return unionId;
	}

	public void setUnionId(String unionId) {
		this.unionId = unionId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getOpenId() {
		return openId;
	}

	public void setOpenId(String openId) {
		this.openId = openId;
	}
}
