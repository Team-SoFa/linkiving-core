package com.sofa.linkiving.domain.link.facade;

import java.net.URI;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.abstraction.ImageUploader;
import com.sofa.linkiving.domain.link.ai.SummaryClient;
import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.dto.internal.LinksDto;
import com.sofa.linkiving.domain.link.dto.internal.OgTagDto;
import com.sofa.linkiving.domain.link.dto.response.LinkCardsRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDetailRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.dto.response.LinkTotalCountRes;
import com.sofa.linkiving.domain.link.dto.response.MetaScrapeRes;
import com.sofa.linkiving.domain.link.dto.response.RagRegenerateSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.RegenerateSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.SummaryRes;
import com.sofa.linkiving.domain.link.dto.response.SummaryStatusRes;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.event.LinkCreatedEvent;
import com.sofa.linkiving.domain.link.event.LinkSyncEvent;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;
import com.sofa.linkiving.domain.link.util.OgTagCrawler;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.analytics.AnalyticsContext;
import com.sofa.linkiving.global.analytics.Ga4Event;
import com.sofa.linkiving.global.analytics.Ga4Publisher;
import com.sofa.linkiving.global.logging.LogContext;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LinkFacade {

	private final LinkService linkService;
	private final OgTagCrawler ogTagCrawler;
	private final SummaryService summaryService;
	private final ImageUploader imageUploader;
	private final ApplicationEventPublisher eventPublisher;
	private final SummaryClient summaryClient;
	private final Ga4Publisher ga4Publisher;

	public LinkRes createLink(
		Member member,
		String url,
		String title,
		String memo,
		String imageUrl,
		AnalyticsContext analyticsContext
	) {
		publishBookmarkSaveAttempt(member, analyticsContext);

		try {
			String storedImageUrl = processImageUpload(imageUrl);
			Link link = linkService.createLink(member, url, title, memo, storedImageUrl);

			eventPublisher.publishEvent(new LinkCreatedEvent(link.getId(), member.getEmail(), LogContext.snapshot()));
			eventPublisher.publishEvent(LinkSyncEvent.createEvent(link));
			publishBookmarkSaveSuccess(member, analyticsContext, link);

			return LinkRes.from(link);
		} catch (RuntimeException exception) {
			publishBookmarkSaveFail(member, analyticsContext, exception);
			throw exception;
		}
	}

	public LinkRes createLink(
		Member member,
		String url,
		String title,
		String memo,
		String imageUrl
	) {
		return createLink(member, url, title, memo, imageUrl, AnalyticsContext.of(null, null));
	}

	public LinkRes createLink(
		Member member,
		String url,
		String title,
		String memo,
		String imageUrl
	) {
		return createLink(member, url, title, memo, imageUrl, AnalyticsContext.of(null, null));
	}

	public LinkRes updateLink(Long linkId, Member member, String title, String memo, String imageUrl) {
		String storedImageUrl = null;
		if (imageUrl != null) {
			storedImageUrl = imageUploader.uploadFromUrl(imageUrl);
		}
		Link link = linkService.updateLink(linkId, member, title, memo, storedImageUrl);
		Summary summary = summaryService.getSummaryOrElseNull(linkId);
		eventPublisher.publishEvent(LinkSyncEvent.updateEvent(link, summary));
		return LinkRes.from(link);
	}

	public LinkRes updateTitle(Long linkId, Member member, String title) {
		Link link = linkService.updateTitle(linkId, member, title);
		Summary summary = summaryService.getSummaryOrElseNull(linkId);
		eventPublisher.publishEvent(LinkSyncEvent.updateEvent(link, summary));
		return LinkRes.from(link);
	}

	public LinkRes updateMemo(Long linkId, Member member, String memo) {
		Link link = linkService.updateMemo(linkId, member, memo);
		Summary summary = summaryService.getSummaryOrElseNull(linkId);
		eventPublisher.publishEvent(LinkSyncEvent.updateEvent(link, summary));
		return LinkRes.from(link);
	}

	public void deleteLink(Long linkId, Member member) {
		linkService.deleteLink(linkId, member);
		eventPublisher.publishEvent(LinkSyncEvent.deleteEvent(linkId));
	}

	@Transactional(readOnly = true)
	public LinkDetailRes getLinkDetail(Long linkId, Member member) {
		LinkDto linkDto = linkService.getLinkWithSummary(linkId, member);
		return LinkDetailRes.from(linkDto);
	}

	@Transactional(readOnly = true)
	public LinkCardsRes getLinkCards(Member member, Long lastId, int size) {
		LinksDto linkDtos = linkService.getLinksWithSummary(member, lastId, size);
		return LinkCardsRes.of(linkDtos);
	}

	@Transactional(readOnly = true)
	public LinkDuplicateCheckRes checkDuplicate(Member member, String url) {
		return linkService.findLinkIdByUrl(member, url)
			.map(LinkDuplicateCheckRes::exists)
			.orElse(LinkDuplicateCheckRes.notExists());
	}

	@Transactional(readOnly = true)
	public RegenerateSummaryRes recreateSummary(Member member, Long linkId, Format format) {
		Link link = linkService.getLinkForSummaryUpdate(linkId, member);

		String url = link.getUrl();
		String existingSummary = summaryService.getSummary(linkId).getContent();

		RagRegenerateSummaryRes res = summaryClient.regenerateSummary(linkId, member.getId(), url, existingSummary);

		return RegenerateSummaryRes.builder()
			.existingSummary(existingSummary)
			.newSummary(res.summary())
			.difference(res.difference())
			.build();
	}

	@Transactional(readOnly = true)
	public MetaScrapeRes scrapeMetadata(String url) {
		OgTagDto ogTag = ogTagCrawler.crawl(url);
		String imageUrl = ogTag.image();
		String uploadedImageUrl = processImageUpload(ogTag.image());

		String responseImageUrl = uploadedImageUrl != null ? uploadedImageUrl : imageUrl;
		return new MetaScrapeRes(
			ogTag.title(),
			ogTag.description(),
			responseImageUrl,
			ogTag.url()
		);
	}

	public SummaryRes updateSummary(Long id, Member member, String content, Format format) {
		Link link = linkService.getLinkForSummaryUpdate(id, member);
		Summary summary = summaryService.createSummary(link, format, content);
		summaryService.selectSummary(link.getId(), summary.getId());
		return SummaryRes.from(summary);
	}

	private String processImageUpload(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) {
			return null;
		}
		return imageUploader.uploadFromUrl(imageUrl);
	}

	private void publishBookmarkSaveAttempt(Member member, AnalyticsContext analyticsContext) {
		publishBookmarkEvent(member, analyticsContext, "bookmark_save_attempt", Map.of(
			"source", analyticsSource(analyticsContext)
		));
	}

	private void publishBookmarkSaveSuccess(Member member, AnalyticsContext analyticsContext, Link link) {
		publishBookmarkEvent(member, analyticsContext, "bookmark_save_success", Map.of(
			"source", analyticsSource(analyticsContext),
			"domain", extractHost(link.getUrl()),
			"has_memo", link.getMemo() != null && !link.getMemo().isBlank()
		));
	}

	private void publishBookmarkSaveFail(Member member, AnalyticsContext analyticsContext, RuntimeException exception) {
		publishBookmarkEvent(member, analyticsContext, "bookmark_save_fail", Map.of(
			"source", analyticsSource(analyticsContext),
			"error_type", exception.getClass().getSimpleName()
		));
	}

	private void publishBookmarkEvent(Member member, AnalyticsContext analyticsContext, String eventName,
		Map<String, Object> params) {
		if (analyticsContext == null || analyticsContext.clientId() == null || analyticsContext.clientId().isBlank()) {
			return;
		}
		String userId = member.getId() == null ? null : String.valueOf(member.getId());
		ga4Publisher.publish(analyticsContext.clientId(), userId, new Ga4Event(eventName, params));
	}

	private String analyticsSource(AnalyticsContext analyticsContext) {
		if (analyticsContext == null || analyticsContext.source() == null || analyticsContext.source().isBlank()) {
			return "web";
		}
		return analyticsContext.source();
	}

	private String extractHost(String url) {
		try {
			String host = URI.create(url).getHost();
			if (host == null || host.isBlank()) {
				return "unknown";
			}
			return host.startsWith("www.") ? host.substring(4) : host;
		} catch (IllegalArgumentException exception) {
			return "unknown";
		}
	}

	@Transactional(readOnly = true)
	public LinkTotalCountRes getLinkTotalCount(Member member) {
		return new LinkTotalCountRes(linkService.getLinkTotalCount(member));
	}

	public void retrySummary(Long id, Member member) {
		linkService.resetSummaryStatusForRetry(id, member);
		eventPublisher.publishEvent(new LinkCreatedEvent(id, member.getEmail(), LogContext.snapshot()));
	}

	@Transactional(readOnly = true)
	public SummaryStatusRes getSummaryStatus(Long id, Member member) {
		LinkDto linkDto = linkService.getLinkWithSummary(id, member);
		SummaryStatus summaryStatus = linkDto.link().getSummaryStatus();

		if (summaryStatus == SummaryStatus.COMPLETED) {
			return SummaryStatusRes.completed(id, SummaryRes.from(linkDto.summary()));
		}

		if (summaryStatus == SummaryStatus.FAILED) {
			return SummaryStatusRes.failed(id, "요약 생성에 실패했습니다.");
		}

		return SummaryStatusRes.of(id, summaryStatus);
	}
}
