package com.sofa.linkiving.domain.link.abstraction;

public interface ImageUploader {
	/**
	 * 외부 이미지 URL을 입력받아 스토리지에 저장하고, 접근 가능한 URL을 반환한다.
	 * 실패 시 null 값을 반환한다 (Soft Fail).
	 */
	String uploadFromUrl(String originalUrl);

	/**
	 * 이미지 URL이 이미 스토리지에 저장되어 있다면 해당 URL을 반환한다.
	 * 저장된 URL이 아니거나 확인할 수 없으면 null을 반환한다.
	 */
	String resolveStoredUrl(String originalUrl);
}
