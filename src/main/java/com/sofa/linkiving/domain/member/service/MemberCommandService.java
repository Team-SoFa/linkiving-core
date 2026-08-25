package com.sofa.linkiving.domain.member.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberCommandService {
	private final MemberRepository memberRepository;

	public Member createOrUpdate(String email) {
		return createOrUpdate(email, null, null);
	}

	public Member createOrUpdate(String email, String name, String profileImageUrl) {
		return memberRepository.findByEmailForUpdate(email)
			.map(member -> {
				member.updateProfile(name, profileImageUrl);
				return member;
			})
			.orElseGet(() -> {
				Member newMember = Member.builder()
					.email(email)
					.name(name)
					.profileImageUrl(profileImageUrl)
					.status(MemberStatus.PENDING_TERMS)
					.build();

				return memberRepository.save(newMember);
			});
	}

	public void delete(Member member) {
		memberRepository.delete(member);
	}
}
