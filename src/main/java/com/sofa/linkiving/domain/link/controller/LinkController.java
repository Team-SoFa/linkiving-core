package com.sofa.linkiving.domain.link.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.link.dto.request.LinkCreateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkMemoUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkTitleUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.MetaScrapeReq;
import com.sofa.linkiving.domain.link.dto.request.RegenerateSummaryReq;
import com.sofa.linkiving.domain.link.dto.request.SummaryUpdateReq;
import com.sofa.linkiving.domain.link.dto.response.LinkCardsRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDetailRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.dto.response.LinkTotalCountRes;
import com.sofa.linkiving.domain.link.dto.response.MetaScrapeRes;
import com.sofa.linkiving.domain.link.dto.response.RegenerateSummaryRes;
import com.sofa.linkiving.domain.link.dto.response.SummaryRes;
import com.sofa.linkiving.domain.link.dto.response.SummaryStatusRes;
import com.sofa.linkiving.domain.link.facade.LinkFacade;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.global.util.HashidsUtils;
import com.sofa.linkiving.security.annotation.AuthMember;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/links")
@RequiredArgsConstructor
public class LinkController implements LinkApi {

	private final LinkFacade linkFacade;
	private final HashidsUtils hashidsUtils;

	@Override
	@PostMapping("/meta-scrape")
	public BaseResponse<MetaScrapeRes> scrapeMetadata(
		@RequestBody MetaScrapeReq request,
		@AuthMember Member member
	) {
		MetaScrapeRes response = linkFacade.scrapeMetadata(request.url());
		return BaseResponse.success(response, "메타 정보 수집 완료");
	}

	@Override
	@GetMapping("/duplicate")
	public BaseResponse<LinkDuplicateCheckRes> checkDuplicate(
		@RequestParam String url,
		@AuthMember Member member
	) {
		LinkDuplicateCheckRes response = linkFacade.checkDuplicate(member, url);
		return BaseResponse.success(response, "URL 중복 체크 완료");
	}

	@Override
	@PostMapping
	public BaseResponse<LinkRes> createLink(
		@RequestBody LinkCreateReq request,
		@AuthMember Member member
	) {
		LinkRes response = linkFacade.createLink(
			member,
			request.url(),
			request.title(),
			request.memo(),
			request.imageUrl()
		);
		return BaseResponse.success(response, "링크 생성 완료");
	}

	@Override
	@PutMapping("/{id}")
	public BaseResponse<LinkRes> updateLink(
		@PathVariable String id,
		@RequestBody LinkUpdateReq request,
		@AuthMember Member member
	) {
		Long realId = hashidsUtils.decode(id);

		LinkRes response = linkFacade.updateLink(
			realId,
			member,
			request.title(),
			request.memo(),
			request.imageUrl()
		);
		return BaseResponse.success(response, "링크 수정 완료");
	}

	@Override
	@DeleteMapping("/{id}")
	public BaseResponse<Void> deleteLink(
		@PathVariable String id,
		@AuthMember Member member
	) {
		Long realId = hashidsUtils.decode(id);
		linkFacade.deleteLink(realId, member);
		return BaseResponse.noContent("링크 삭제 완료");
	}

	@Override
	@GetMapping("/{id}")
	public BaseResponse<LinkDetailRes> getLink(
		@PathVariable String id,
		@AuthMember Member member
	) {
		Long realId = hashidsUtils.decode(id);
		LinkDetailRes response = linkFacade.getLinkDetail(realId, member);
		return BaseResponse.success(response, "링크 조회 완료");
	}

	@Override
	@GetMapping
	public BaseResponse<LinkCardsRes> getLinkList(
		@AuthMember Member member,
		@RequestParam(required = false) String lastId,
		@RequestParam(defaultValue = "20") int size
	) {
		Long realLastId = hashidsUtils.decode(lastId);
		LinkCardsRes response = linkFacade.getLinkCards(member, realLastId, size);
		return BaseResponse.success(response, "링크 목록 조회 완료");
	}

	@Override
	@PatchMapping("/{id}/title")
	public BaseResponse<LinkRes> updateTitle(
		@PathVariable String id,
		@RequestBody LinkTitleUpdateReq request,
		@AuthMember Member member
	) {
		Long realId = hashidsUtils.decode(id);
		LinkRes response = linkFacade.updateTitle(realId, member, request.title());
		return BaseResponse.success(response, "제목 수정 완료");
	}

	@Override
	@PatchMapping("/{id}/memo")
	public BaseResponse<LinkRes> updateMemo(
		@PathVariable String id,
		@RequestBody LinkMemoUpdateReq request,
		@AuthMember Member member
	) {
		Long realId = hashidsUtils.decode(id);
		LinkRes response = linkFacade.updateMemo(realId, member, request.memo());
		return BaseResponse.success(response, "메모 수정 완료");
	}

	@Override
	@PostMapping("/{id}/summary")
	public BaseResponse<RegenerateSummaryRes> recreateSummary(
		@PathVariable String id,
		@RequestBody RegenerateSummaryReq req,
		@AuthMember Member member
	) {
		Long realId = hashidsUtils.decode(id);
		RegenerateSummaryRes response = linkFacade.recreateSummary(member, realId, req.format());
		return BaseResponse.success(response, "요약 재성성 완료");
	}

	@Override
	@PatchMapping("/{id}/summary")
	public BaseResponse<SummaryRes> updateSummary(
		@PathVariable String id,
		@RequestBody SummaryUpdateReq request,
		@AuthMember Member member
	) {
		Long realId = hashidsUtils.decode(id);
		SummaryRes response = linkFacade.updateSummary(realId, member, request.summary(), request.format());
		return BaseResponse.success(response, "요약 수정 완료");
	}

	@Override
	@GetMapping("/count")
	public BaseResponse<LinkTotalCountRes> getLinkTotalCount(@AuthMember Member member) {
		LinkTotalCountRes res = linkFacade.getLinkTotalCount(member);
		return BaseResponse.success(res, "링크 전체 개수 조회 완료");
	}

	@Override
	@PostMapping("/{id}/retry-summary")
	public BaseResponse<Void> retrySummary(
		@PathVariable String id,
		@AuthMember Member member
	) {
		Long realId = hashidsUtils.decode(id);
		linkFacade.retrySummary(realId, member);
		return BaseResponse.noContent("요약 재시도");
	}

	@Override
	@GetMapping("/{id}/summary-status")
	public BaseResponse<SummaryStatusRes<?>> getSummaryStatus(
		@PathVariable String id,
		@AuthMember Member member
	) {
		Long realId = hashidsUtils.decode(id);
		SummaryStatusRes response = linkFacade.getSummaryStatus(realId, member);
		return BaseResponse.success(response, "요약 상태 조회 완료");
	}
}
