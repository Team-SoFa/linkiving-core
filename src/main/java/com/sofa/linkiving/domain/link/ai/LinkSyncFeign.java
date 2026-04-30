package com.sofa.linkiving.domain.link.ai;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncDeleteReq;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;

@FeignClient(name = "linkSyncClient", url = "${ai.server.url}")
public interface LinkSyncFeign {

	@PostMapping("/webhook/update-link")
	void syncUpdate(@RequestBody LinkSyncUpdateReq req);

	@DeleteMapping("/webhook/link-delete")
	void syncDelete(@RequestBody LinkSyncDeleteReq req);
}
