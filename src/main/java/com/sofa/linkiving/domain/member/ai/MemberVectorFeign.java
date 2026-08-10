package com.sofa.linkiving.domain.member.ai;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.sofa.linkiving.infra.feign.GlobalFeignConfig;

@FeignClient(name = "member-vector-client", url = "${ai.server.url}", configuration = GlobalFeignConfig.class)
public interface MemberVectorFeign {

	@DeleteMapping("/webhook/member-delete")
	void deleteAll(
		@RequestHeader("x-linkiving-internal-secret") String internalSecret,
		@RequestBody MemberVectorDeleteReq request
	);
}
