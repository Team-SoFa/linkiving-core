package com.sofa.linkiving.domain.link.abstraction;

public interface ImageUploader {
	/**
	 * 외부 이미지 URL을 입력받아 스토리지에 저장하고, 접근 가능한 URL을 반환한다.
	 * 실패 시 null 값을 반환한다 (Soft Fail).
	 */
	String uploadFromUrl(String originalUrl);
}
