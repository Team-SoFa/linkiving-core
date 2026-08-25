package com.sofa.linkiving.domain.member.entity;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.common.BaseEntity;
import com.sofa.linkiving.global.error.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
	private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z]{2,6}$";
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);

	@Column(nullable = false, unique = true)
	private String email;
	@Column
	private String name;
	@Column(length = 2048)
	private String profileImageUrl;
	@Column(nullable = false)
	private Role role;
	@Column(nullable = false, columnDefinition = "integer default 2")
	private MemberStatus status;
	@Column
	private LocalDateTime termsAgreedAt;
	@Column
	private LocalDateTime privacyAgreedAt;
	@Column
	private String termsVersion;
	@Column
	private String privacyVersion;

	@Builder
	public Member(String email, String name, String profileImageUrl, MemberStatus status) {
		if (!isValidEmail(email)) {
			throw new BusinessException(MemberErrorCode.INVALID_EMAIL_FORMAT);
		}
		this.email = email;
		this.name = name;
		this.profileImageUrl = profileImageUrl;
		this.role = Role.USER;
		this.status = status == null ? MemberStatus.ACTIVE : status;
	}

	private boolean isValidEmail(String email) {
		return EMAIL_PATTERN.matcher(email).matches();
	}

	public MemberStatus getStatus() {
		return status == null ? MemberStatus.ACTIVE : status;
	}

	@PrePersist
	private void setDefaultStatus() {
		if (status == null) {
			status = MemberStatus.ACTIVE;
		}
	}

	public void updateProfile(String name, String profileImageUrl) {
		if (name != null) {
			this.name = name;
		}
		if (profileImageUrl != null) {
			this.profileImageUrl = profileImageUrl;
		}
	}

	public boolean needsTermsAgreement() {
		return getStatus() == MemberStatus.PENDING_TERMS;
	}

	public boolean hasAgreedTerms() {
		return getStatus() == MemberStatus.ACTIVE;
	}

	public void agreeTerms(String termsVersion, String privacyVersion) {
		if (!needsTermsAgreement()) {
			throw new BusinessException(MemberErrorCode.TERMS_ALREADY_AGREED);
		}

		LocalDateTime now = LocalDateTime.now();
		this.termsAgreedAt = now;
		this.privacyAgreedAt = now;
		this.termsVersion = termsVersion;
		this.privacyVersion = privacyVersion;
		this.status = MemberStatus.ACTIVE;
	}

	public boolean isWithdrawing() {
		return getStatus() == MemberStatus.WITHDRAWING
			|| getStatus() == MemberStatus.WITHDRAWAL_ANALYTICS_SENT;
	}

	public void beginWithdrawal() {
		this.status = MemberStatus.WITHDRAWING;
	}

	public boolean claimWithdrawalAnalytics() {
		if (getStatus() != MemberStatus.WITHDRAWING) {
			return false;
		}
		this.status = MemberStatus.WITHDRAWAL_ANALYTICS_SENT;
		return true;
	}
}
