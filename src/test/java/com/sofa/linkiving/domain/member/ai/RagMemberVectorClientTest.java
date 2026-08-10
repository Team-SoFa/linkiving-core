package com.sofa.linkiving.domain.member.ai;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.member.config.MemberWithdrawalProperties;

@ExtendWith(MockitoExtension.class)
class RagMemberVectorClientTest {

	@Mock MemberVectorFeign memberVectorFeign;
	private RagMemberVectorClient memberVectorClient;

	@Test
	void shouldRequestMemberVectorDeletion() {
		memberVectorClient = new RagMemberVectorClient(memberVectorFeign,
			new MemberWithdrawalProperties(true, "internal-secret", Duration.ofMinutes(10)));
		memberVectorClient.deleteAll(42L);

		verify(memberVectorFeign).deleteAll(org.mockito.ArgumentMatchers.eq("internal-secret"),
			argThat(request -> request.userId().equals(42L)));
	}
}
