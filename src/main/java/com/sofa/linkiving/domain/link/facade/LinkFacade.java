package com.sofa.linkiving.domain.link.facade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.dto.OgTagDto;
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

	public LinkRes createLink(Member member, String url, String title, String memo, String imageUrl) {
		Link link = linkService.createLink(member, url, title, memo, imageUrl);
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
	public LinkRes getLink(Long linkId, Member member) {
		Link link = linkService.getLink(linkId, member);
		return LinkRes.from(link);
	}

	@Transactional(readOnly = true)
	public Page<LinkRes> getLinkList(Member member, Pageable pageable) {
		Page<Link> links = linkService.getLinkList(member, pageable);
		return links.map(LinkRes::from);
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
