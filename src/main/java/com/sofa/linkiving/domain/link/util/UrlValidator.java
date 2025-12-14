package com.sofa.linkiving.domain.link.util;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

/**
 * SSRF(Server-Side Request Forgery) 공격을 방어하기 위한 URL 검증 유틸리티
 */
@Slf4j
@Component
public class UrlValidator {

	private static final String[] ALLOWED_PROTOCOLS = {"http", "https"};

	/**
	 * SSRF 공격으로부터 안전한 URL인지 검증
	 *
	 * @param urlString 검증할 URL
	 * @throws BusinessException URL이 안전하지 않은 경우
	 */
	public void validateSafeUrl(String urlString) {
		try {
			URL url = new URL(urlString);

			// 1. 프로토콜 검증 (http, https만 허용)
			validateProtocol(url);

			// 2. 호스트 검증
			String host = url.getHost();
			validateHost(host);

			// 3. IP 주소 검증 (Private IP, Loopback, Link-local 차단)
			validateIpAddress(host);

		} catch (MalformedURLException e) {
			log.warn("잘못된 URL 형식: {}", urlString);
			throw new BusinessException(LinkErrorCode.INVALID_URL);
		} catch (UnknownHostException e) {
			log.warn("호스트를 찾을 수 없음: {}", urlString);
			throw new BusinessException(LinkErrorCode.INVALID_URL);
		}
	}

	private void validateProtocol(URL url) {
		String protocol = url.getProtocol().toLowerCase();
		boolean isAllowed = false;

		for (String allowedProtocol : ALLOWED_PROTOCOLS) {
			if (allowedProtocol.equals(protocol)) {
				isAllowed = true;
				break;
			}
		}

		if (!isAllowed) {
			log.warn("허용되지 않은 프로토콜: {}", protocol);
			throw new BusinessException(LinkErrorCode.INVALID_URL_PROTOCOL);
		}
	}

	private void validateHost(String host) {
		if (host == null || host.isEmpty()) {
			log.warn("호스트가 비어있음");
			throw new BusinessException(LinkErrorCode.INVALID_URL);
		}

		// localhost, 0.0.0.0 차단
		String lowerHost = host.toLowerCase();
		if (lowerHost.equals("localhost")
			|| lowerHost.equals("0.0.0.0")
			|| lowerHost.startsWith("127.")
			|| lowerHost.equals("::1")
			|| lowerHost.equals("0:0:0:0:0:0:0:1")) {
			log.warn("Loopback 주소 접근 시도: {}", host);
			throw new BusinessException(LinkErrorCode.INVALID_URL_PRIVATE_IP);
		}
	}

	private void validateIpAddress(String host) throws UnknownHostException {
		// DNS 조회하여 실제 IP 주소 확인
		InetAddress address = InetAddress.getByName(host);

		// Private IP 대역 차단
		if (address.isSiteLocalAddress()) {
			// 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
			log.warn("Private IP 접근 시도: {} -> {}", host, address.getHostAddress());
			throw new BusinessException(LinkErrorCode.INVALID_URL_PRIVATE_IP);
		}

		// Loopback 주소 차단 (127.0.0.0/8)
		if (address.isLoopbackAddress()) {
			log.warn("Loopback IP 접근 시도: {} -> {}", host, address.getHostAddress());
			throw new BusinessException(LinkErrorCode.INVALID_URL_PRIVATE_IP);
		}

		// Link-local 주소 차단 (169.254.0.0/16, AWS 메타데이터 서버 포함)
		if (address.isLinkLocalAddress()) {
			log.warn("Link-local IP 접근 시도: {} -> {}", host, address.getHostAddress());
			throw new BusinessException(LinkErrorCode.INVALID_URL_PRIVATE_IP);
		}

		// AWS 메타데이터 서버 명시적 차단
		String ipAddress = address.getHostAddress();
		if (ipAddress.startsWith("169.254.169.254")) {
			log.warn("AWS 메타데이터 서버 접근 시도: {}", ipAddress);
			throw new BusinessException(LinkErrorCode.INVALID_URL_PRIVATE_IP);
		}
	}
}
