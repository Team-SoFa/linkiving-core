package com.sofa.linkiving.domain.link.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.sofa.linkiving.domain.link.dto.request.LinkCreateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkMemoUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkTitleUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkUpdateReq;
import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseResponse;
import com.sofa.linkiving.security.annotation.AuthMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Link", description = "링크 관리 API")
public interface LinkApi {

	@Operation(summary = "URL 중복 체크", description = "저장하려는 URL이 이미 존재하는지 확인하고, 존재 시 linkId를 반환합니다")
	ResponseEntity<BaseResponse<LinkDuplicateCheckRes>> checkDuplicate(
		@RequestParam String url,
		@AuthMember Member member
	);

	@Operation(summary = "링크 생성", description = "새로운 링크를 저장합니다")
	ResponseEntity<BaseResponse<LinkRes>> createLink(
		@Valid @RequestBody LinkCreateReq request,
		@AuthMember Member member
	);

	@Operation(summary = "링크 수정", description = "링크 정보를 수정합니다. null이 아닌 필드만 수정됩니다.")
	ResponseEntity<BaseResponse<LinkRes>> updateLink(
		@PathVariable Long id,
		@Valid @RequestBody LinkUpdateReq request,
		@AuthMember Member member
	);

	@Operation(summary = "링크 삭제", description = "링크를 삭제합니다 (Soft Delete)")
	ResponseEntity<BaseResponse<Void>> deleteLink(
		@PathVariable Long id,
		@AuthMember Member member
	);

	@Operation(summary = "링크 조회", description = "링크 상세 정보를 조회합니다")
	ResponseEntity<BaseResponse<LinkRes>> getLink(
		@PathVariable Long id,
		@AuthMember Member member
	);

	@Operation(summary = "링크 목록 조회", description = "저장된 링크 목록을 페이징하여 조회합니다")
	ResponseEntity<BaseResponse<Page<LinkRes>>> getLinkList(
		Pageable pageable,
		@AuthMember Member member
	);

	@Operation(summary = "링크 제목 수정", description = "링크 제목만 수정합니다")
	ResponseEntity<BaseResponse<LinkRes>> updateTitle(
		@PathVariable Long id,
		@Valid @RequestBody LinkTitleUpdateReq request,
		@AuthMember Member member
	);

	@Operation(summary = "링크 메모 수정", description = "링크 메모만 수정합니다")
	ResponseEntity<BaseResponse<LinkRes>> updateMemo(
		@PathVariable Long id,
		@Valid @RequestBody LinkMemoUpdateReq request,
		@AuthMember Member member
	);
}
