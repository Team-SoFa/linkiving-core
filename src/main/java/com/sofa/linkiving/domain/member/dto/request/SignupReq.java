package com.sofa.linkiving.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupReq(
	@Schema(description = "이메일")
	@Email
	@NotBlank
	String email,
	@Schema(description = "비밀번호")
	@NotBlank
	String password
) {
}
