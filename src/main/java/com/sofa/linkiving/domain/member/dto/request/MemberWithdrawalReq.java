package com.sofa.linkiving.domain.member.dto.request;

import com.sofa.linkiving.domain.member.enums.MemberDeleteReason;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MemberWithdrawalReq(
	@NotNull
	@AssertTrue
	Boolean confirmed,
	@NotNull
	MemberDeleteReason deleteReason,
	@NotBlank
	@Pattern(regexp = "^\\d+\\.\\d+$")
	String clientId
) {
}
