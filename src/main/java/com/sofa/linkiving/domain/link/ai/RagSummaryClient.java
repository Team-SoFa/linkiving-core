package com.sofa.linkiving.domain.link.ai;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.domain.link.dto.request.RagInitialSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.RagRegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RagSummaryClient implements SummaryClient {

	private final RagSummaryFeign ragSummaryFeign;

	@Override
	public RagInitialSummaryRes initialSummary(Long linkId, Long userId, String title, String url, String memo) {
		try {
			RagInitialSummaryReq req = new RagInitialSummaryReq(linkId, userId, title, url, memo);
			List<RagInitialSummaryRes> response = ragSummaryFeign.requestInitialSummary(req);

			if (response != null && !response.isEmpty()) {
				log.info("[AI Server]  Initial Summary Requested Success. LinkId: {}", linkId);
				return response.get(0);
			}
			return null;

		} catch (Exception e) {
			log.error("[AI Server Error] Failed to request initial summary for LinkId: {}. Error: {}", linkId,
				e.getMessage());
			return null;
		}
	}

	@Override
	public RagRegenerateSummaryRes regenerateSummary(Long linkId, Long userId, String url, String existingSummary) {
		try {
			RagRegenerateSummaryReq req = new RagRegenerateSummaryReq(linkId, userId, url, existingSummary);
			List<RagRegenerateSummaryRes> response = ragSummaryFeign.requestRegenerateSummary(req);

			if (response != null && !response.isEmpty()) {
				log.info("[AI Server] Regenerate Summary Success. LinkId: {}", linkId);
				return response.get(0);
			}
			return null;

		} catch (Exception e) {
			log.error("[AI Server Error] Failed to regenerate summary for LinkId: {}. Error: {}", linkId,
				e.getMessage());
			return null;
		}
	}
}
