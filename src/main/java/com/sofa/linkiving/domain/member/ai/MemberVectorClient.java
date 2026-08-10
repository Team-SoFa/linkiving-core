package com.sofa.linkiving.domain.member.ai;

public interface MemberVectorClient {

	void validateConfiguration();

	void deleteAll(Long memberId);
}
