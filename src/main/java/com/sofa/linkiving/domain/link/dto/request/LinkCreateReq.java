package com.sofa.linkiving.domain.link.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkCreateReq(
	@Schema(description = "Link URL", example = "https://example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "URL is required")
	@Size(max = 2048, message = "URL must be 2048 characters or less")
	String url,

	@Schema(
		description = "Link title",
		example = "Useful development resource",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	@NotBlank(message = "Title is required")
	@Size(max = 100, message = "Title must be 100 characters or less")
	String title,

	@Schema(description = "Memo", example = "Read later")
	String memo,

	@Schema(description = "Image URL", example = "https://example.com/image.jpg")
	String imageUrl,

	@Schema(description = "GA4 client_id", example = "1234567890.1234567890")
	String clientId,

	@Schema(description = "Link save source", example = "web")
	String source
) {
	public LinkCreateReq(String url, String title, String memo, String imageUrl) {
		this(url, title, memo, imageUrl, null, null);
	}
}
