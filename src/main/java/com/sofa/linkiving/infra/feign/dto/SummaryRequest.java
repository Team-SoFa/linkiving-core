package com.sofa.linkiving.infra.feign.dto;

public record SummaryRequest(
	Long linkId,
	Long userId,
	String url,
	String title,
	String memo
) {
	public static SummaryRequest of(Long linkId, Long userId, String url, String title, String memo) {
		return new SummaryRequest(linkId, userId, url, title, memo);
	}
}
