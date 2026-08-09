package com.sofa.linkiving.domain.member.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record TermsAgreementReq(
	@AssertTrue
	boolean termsAgreed,
	@AssertTrue
	boolean privacyAgreed,
	@NotBlank
	String termsVersion,
	@NotBlank
	String privacyVersion
) {
}
