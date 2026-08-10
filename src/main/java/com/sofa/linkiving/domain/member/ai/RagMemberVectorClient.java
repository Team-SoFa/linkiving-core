package com.sofa.linkiving.domain.member.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sofa.linkiving.domain.member.config.MemberWithdrawalProperties;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RagMemberVectorClient implements MemberVectorClient {

	private final MemberVectorFeign memberVectorFeign;
	private final MemberWithdrawalProperties properties;

	@Override
	public void validateConfiguration() {
		if (!StringUtils.hasText(properties.internalSecret())) {
			throw new BusinessException(MemberErrorCode.AI_SERVICE_AUTH_NOT_CONFIGURED);
		}
	}

	@Override
	public void deleteAll(Long memberId) {
		validateConfiguration();
		memberVectorFeign.deleteAll(properties.internalSecret(), new MemberVectorDeleteReq(memberId));
	}
}
