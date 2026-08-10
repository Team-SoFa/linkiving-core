package com.sofa.linkiving.domain.member.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class MockMemberVectorClient implements MemberVectorClient {

	@Override
	public void validateConfiguration() {
	}

	@Override
	public void deleteAll(Long memberId) {
	}
}
