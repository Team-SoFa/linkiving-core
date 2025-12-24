package com.sofa.linkiving.domain.link.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.dto.response.MetaScrapeRes;
import com.sofa.linkiving.domain.link.dto.response.RecreateSummaryResponse;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.facade.LinkFacade;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.security.annotation.AuthMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/links")
@RequiredArgsConstructor
public class LinkController implements LinkApi {

	private final LinkFacade linkFacade;

	@Override
	@PostMapping("/meta-scrape")
	public BaseResponse<MetaScrapeRes> scrapeMetadata(
		@Valid @RequestBody MetaScrapeReq request,
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
		@Valid @RequestBody LinkCreateReq request,
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
		@PathVariable Long id,
		@Valid @RequestBody LinkUpdateReq request,
		@AuthMember Member member
	) {
		LinkRes response = linkFacade.updateLink(
			id,
			member,
			request.title(),
			request.memo()
		);
		return BaseResponse.success(response, "링크 수정 완료");
	}

	@Override
	@DeleteMapping("/{id}")
	public BaseResponse<Void> deleteLink(
		@PathVariable Long id,
		@AuthMember Member member
	) {
		linkFacade.deleteLink(id, member);
		return BaseResponse.noContent("링크 삭제 완료");
	}

	@Override
	@GetMapping("/{id}")
	public BaseResponse<LinkRes> getLink(
		@PathVariable Long id,
		@AuthMember Member member
	) {
		LinkRes response = linkFacade.getLink(id, member);
		return BaseResponse.success(response, "링크 조회 완료");
	}

	@Override
	@GetMapping
	public BaseResponse<Page<LinkRes>> getLinkList(
		@PageableDefault(size = 20) Pageable pageable,
		@AuthMember Member member
	) {
		Page<LinkRes> response = linkFacade.getLinkList(member, pageable);
		return BaseResponse.success(response, "링크 목록 조회 완료");
	}

	@Override
	@PatchMapping("/{id}/title")
	public BaseResponse<LinkRes> updateTitle(
		@PathVariable Long id,
		@Valid @RequestBody LinkTitleUpdateReq request,
		@AuthMember Member member
	) {
		LinkRes response = linkFacade.updateTitle(id, member, request.title());
		return BaseResponse.success(response, "제목 수정 완료");
	}

	@Override
	@PatchMapping("/{id}/memo")
	public BaseResponse<LinkRes> updateMemo(
		@PathVariable Long id,
		@Valid @RequestBody LinkMemoUpdateReq request,
		@AuthMember Member member
	) {
		LinkRes response = linkFacade.updateMemo(id, member, request.memo());
		return BaseResponse.success(response, "메모 수정 완료");
	}

	@Override
	@GetMapping("/{id}/summary")
	public BaseResponse<RecreateSummaryResponse> recreateSummary(
		@PathVariable Long id,
		@Valid @RequestParam Format format,
		@AuthMember Member member
	) {
		RecreateSummaryResponse response = linkFacade.recreateSummary(member, id, format);
		return BaseResponse.success(response, "요약 재성성 완료");
	}
}
