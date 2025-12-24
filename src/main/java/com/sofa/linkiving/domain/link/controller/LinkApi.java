package com.sofa.linkiving.domain.link.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Link", description = "링크 관리 API")
public interface LinkApi {

	@Operation(summary = "메타 정보 수집", description = "URL의 OG 태그를 크롤링하여 메타 정보를 반환합니다")
	BaseResponse<MetaScrapeRes> scrapeMetadata(
		MetaScrapeReq request,
		Member member
	);

	@Operation(summary = "URL 중복 체크", description = "저장하려는 URL이 이미 존재하는지 확인하고, 존재 시 linkId를 반환합니다")
	BaseResponse<LinkDuplicateCheckRes> checkDuplicate(
		String url,
		Member member
	);

	@Operation(summary = "링크 생성", description = "새로운 링크를 저장합니다")
	BaseResponse<LinkRes> createLink(
		LinkCreateReq request,
		Member member
	);

	@Operation(summary = "링크 수정", description = "링크 정보를 수정합니다. null이 아닌 필드만 수정됩니다.")
	BaseResponse<LinkRes> updateLink(
		Long id,
		LinkUpdateReq request,
		Member member
	);

	@Operation(summary = "링크 삭제", description = "링크를 삭제합니다 (Soft Delete)")
	BaseResponse<Void> deleteLink(
		Long id,
		Member member
	);

	@Operation(summary = "링크 조회", description = "링크 상세 정보를 조회합니다")
	BaseResponse<LinkRes> getLink(
		Long id,
		Member member
	);

	@Operation(summary = "링크 목록 조회", description = "저장된 링크 목록을 페이징하여 조회합니다")
	BaseResponse<Page<LinkRes>> getLinkList(
		Pageable pageable,
		Member member
	);

	@Operation(summary = "링크 제목 수정", description = "링크 제목만 수정합니다")
	BaseResponse<LinkRes> updateTitle(
		Long id,
		LinkTitleUpdateReq request,
		Member member
	);

	@Operation(summary = "링크 메모 수정", description = "링크 메모만 수정합니다")
	BaseResponse<LinkRes> updateMemo(
		Long id,
		LinkMemoUpdateReq request,
		Member member
	);

	@Operation(summary = "요약 재생성", description = "요약을 재생성 하고 신규 요약 기존 요약, 기존 및 신규 요약 비교 정보을 제공합니다.")
	BaseResponse<RecreateSummaryResponse> recreateSummary(
		Long id,
		@Schema(description = "요청 형식(CONCISE: 간결하게, DETAILED:자세하게)") Format format,
		Member member
	);
}
