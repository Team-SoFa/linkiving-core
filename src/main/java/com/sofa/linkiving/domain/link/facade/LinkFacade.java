package com.sofa.linkiving.domain.link.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.abstraction.ImageUploader;
import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.dto.internal.LinksDto;
import com.sofa.linkiving.domain.link.dto.internal.OgTagDto;
import com.sofa.linkiving.domain.link.dto.response.LinkCardsRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDetailRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.dto.response.MetaScrapeRes;
import com.sofa.linkiving.domain.link.dto.response.RecreateSummaryResponse;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;
import com.sofa.linkiving.domain.link.util.OgTagCrawler;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LinkFacade {

	private final LinkService linkService;
	private final OgTagCrawler ogTagCrawler;
	private final SummaryService summaryService;
	private final ImageUploader imageUploader;

	public LinkRes createLink(Member member, String url, String title, String memo, String imageUrl) {
		String storedImageUrl = imageUploader.uploadFromUrl(imageUrl);
		Link link = linkService.createLink(member, url, title, memo, storedImageUrl);
		return LinkRes.from(link);
	}

	public LinkRes updateLink(Long linkId, Member member, String title, String memo) {
		Link link = linkService.updateLink(linkId, member, title, memo);
		return LinkRes.from(link);
	}

	public LinkRes updateTitle(Long linkId, Member member, String title) {
		Link link = linkService.updateTitle(linkId, member, title);
		return LinkRes.from(link);
	}

	public LinkRes updateMemo(Long linkId, Member member, String memo) {
		Link link = linkService.updateMemo(linkId, member, memo);
		return LinkRes.from(link);
	}

	public void deleteLink(Long linkId, Member member) {
		linkService.deleteLink(linkId, member);
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
	public RecreateSummaryResponse recreateSummary(Member member, Long linkId, Format format) {

		String url = linkService.getLink(linkId, member).getUrl();

		String existingSummary = summaryService.getSummary(linkId).getContent();
		String newSummary = summaryService.createSummary(linkId, url, format);

		String comparison = summaryService.comparisonSummary(existingSummary, newSummary);

		return RecreateSummaryResponse.builder()
			.existingSummary(existingSummary)
			.newSummary(newSummary)
			.comparison(comparison)
			.build();
	}

	@Transactional(readOnly = true)
	public MetaScrapeRes scrapeMetadata(String url) {
		OgTagDto ogTag = ogTagCrawler.crawl(url);
		return MetaScrapeRes.from(ogTag);
	}
}
