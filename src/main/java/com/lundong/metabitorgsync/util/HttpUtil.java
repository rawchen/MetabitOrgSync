package com.lundong.metabitorgsync.util;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author RawChen
 * @date 2023-07-13 15:49
 */
public class HttpUtil {
	/**
	 * 获取ip
	 */
	public static String getIp(HttpServletRequest request) {
		List<String> ipHeadList = Stream.of("X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "X-Real-IP").collect(Collectors.toList());
		for (String ipHead : ipHeadList) {
			if (checkIP(request.getHeader(ipHead))) {
				return request.getHeader(ipHead).split(",")[0];
			}
		}
		return "0:0:0:0:0:0:0:1".equals(request.getRemoteAddr()) ? "127.0.0.1" : request.getRemoteAddr();

	}

	/**
	 * 检查ip存在
	 */
	public static boolean checkIP(String ip) {
		return !(null == ip || 0 == ip.length() || "unknown".equalsIgnoreCase(ip));
	}


}
