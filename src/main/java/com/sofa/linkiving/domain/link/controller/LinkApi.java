package com.sofa.linkiving.domain.link.controller;

import org.springframework.validation.annotation.Validated;

import com.sofa.linkiving.domain.link.dto.request.LinkCreateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkMemoUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkTitleUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.MetaScrapeReq;
import com.sofa.linkiving.domain.link.dto.request.SummaryUpdateReq;
import com.sofa.linkiving.domain.link.dto.response.LinkCardsRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDetailRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.dto.response.MetaScrapeRes;
import com.sofa.linkiving.domain.link.dto.response.RecreateSummaryResponse;
import com.sofa.linkiving.domain.link.dto.response.SummaryRes;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@Tag(name = "Link", description = "링크 관리 API")
public interface LinkApi {

	@Operation(summary = "메타 정보 수집", description = "URL의 OG 태그를 크롤링하여 메타 정보를 반환합니다")
	BaseResponse<MetaScrapeRes> scrapeMetadata(
		@Valid MetaScrapeReq request,
		Member member
	);

	@Operation(summary = "URL 중복 체크", description = "저장하려는 URL이 이미 존재하는지 확인하고, 존재 시 linkId를 반환합니다")
	BaseResponse<LinkDuplicateCheckRes> checkDuplicate(
		String url,
		Member member
	);

	@Operation(summary = "링크 생성", description = "새로운 링크를 저장합니다")
	BaseResponse<LinkRes> createLink(
		@Valid LinkCreateReq request,
		Member member
	);

	@Operation(summary = "링크 수정", description = "링크 정보를 수정합니다. null이 아닌 필드만 수정됩니다.")
	BaseResponse<LinkRes> updateLink(
		Long id,
		@Valid LinkUpdateReq request,
		Member member
	);

	@Operation(summary = "링크 삭제", description = "링크를 삭제합니다 (Soft Delete)")
	BaseResponse<Void> deleteLink(
		Long id,
		Member member
	);

	@Operation(summary = "링크 상세 조회", description = "링크 상세 정보를 조회합니다")
	BaseResponse<LinkDetailRes> getLink(
		Long id,
		Member member
	);

	@Operation(summary = "링크 카드 목록 조회", description = "저장된 링크 목록을 무한 스크롤 방식으로 조회합니다")
	BaseResponse<LinkCardsRes> getLinkList(
		Member member,
		@Parameter(description = "페이징을 위한 마지막 메시지 ID, 첫 조회 시 null") Long lastId,

		@Parameter(description = "한번에 조회할 데이터 갯수")
		@Min(value = 1, message = "최소 1개 이상 조회해야 합니다.")
		@Max(value = 50, message = "한 번에 최대 50개까지만 조회할 수 있습니다.")
		int size
	);

	@Operation(summary = "링크 제목 수정", description = "링크 제목만 수정합니다")
	BaseResponse<LinkRes> updateTitle(
		Long id,
		@Valid LinkTitleUpdateReq request,
		Member member
	);

	@Operation(summary = "링크 메모 수정", description = "링크 메모만 수정합니다")
	BaseResponse<LinkRes> updateMemo(
		Long id,
		@Valid LinkMemoUpdateReq request,
		Member member
	);

	@Operation(summary = "요약 재생성", description = "요약을 재생성 하고 신규 요약 기존 요약, 기존 및 신규 요약 비교 정보을 제공합니다.")
	BaseResponse<RecreateSummaryResponse> recreateSummary(
		Long id,
		@Schema(description = "요청 형식(CONCISE: 간결하게, DETAILED:자세하게)")
		@Valid Format format,
		Member member
	);

	@Operation(summary = "새로운 요약 선택", description = "신규 요약으로 요약 내용을 수정합니다.")
	BaseResponse<SummaryRes> updateSummary(
		Long id,
		@Valid SummaryUpdateReq request,
		Member member
	);
}
