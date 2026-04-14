package com.sofa.linkiving.domain.member.service;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberCommandService {
	private final MemberRepository memberRepository;

	public Member addUser(String email, String password) {
		Member member = Member.builder()
			.email(email)
			.password(password)
			.build();

		return memberRepository.save(member);
	}

	public Member createOrUpdate(String email) {
		return createOrUpdate(email, null, null);
	}

	public Member createOrUpdate(String email, String name, String profileImageUrl) {
		return memberRepository.findByEmail(email)
			.map(member -> {
				member.updateProfile(name, profileImageUrl);
				return member;
			})
			.orElseGet(() -> {
				Member newMember = Member.builder()
					.email(email)
					.password(email)
					.name(name)
					.profileImageUrl(profileImageUrl)
					.build();

				return memberRepository.save(newMember);
			});
	}
}
