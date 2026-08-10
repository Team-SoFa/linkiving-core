package com.sofa.linkiving.domain.member.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.entity.SummaryDeadLetter;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.link.repository.SummaryDeadLetterRepository;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.domain.member.service.MemberCommandService;
import com.sofa.linkiving.domain.member.service.MemberDataDeletionService;
import com.sofa.linkiving.domain.member.service.MemberQueryService;
import com.sofa.linkiving.domain.report.entity.Report;
import com.sofa.linkiving.domain.report.repository.ReportRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@TestPropertySource(properties = {
	"ai.server.url=http://localhost",
	"test.external.base-url=localhost"
})
class MemberDataDeletionIntegrationTest {

	@Autowired MemberRepository memberRepository;
	@Autowired LinkRepository linkRepository;
	@Autowired SummaryRepository summaryRepository;
	@Autowired SummaryDeadLetterRepository summaryDeadLetterRepository;
	@Autowired ChatRepository chatRepository;
	@Autowired MessageRepository messageRepository;
	@Autowired FeedbackRepository feedbackRepository;
	@Autowired ReportRepository reportRepository;
	@Autowired EntityManager entityManager;

	@Test
	void shouldHardDeleteEveryMemberOwnedRecord() {
		Member member = memberRepository.save(Member.builder()
			.email("withdraw@test.com").password("pw").build());
		Link link = linkRepository.save(Link.create(member, "https://example.com", "title", "memo", null));
		summaryRepository.save(Summary.builder().link(link).format(Format.CONCISE).content("summary")
			.selected(true).build());
		summaryDeadLetterRepository.save(SummaryDeadLetter.builder().linkId(link.getId()).memberId(member.getId())
			.failureReason("failed").build());
		Chat chat = chatRepository.save(Chat.builder().member(member).title("chat").build());
		Message message = messageRepository.save(Message.builder().chat(chat).type(Type.AI).content("answer")
			.links(List.of(link)).build());
		feedbackRepository.save(Feedback.builder().message(message).text("good").sentiment(Sentiment.LIKE).build());
		reportRepository.save(Report.builder().member(member).content("report").build());
		entityManager.flush();

		MemberDataDeletionService service = new MemberDataDeletionService(
			feedbackRepository, messageRepository, chatRepository, summaryRepository, summaryDeadLetterRepository,
			linkRepository, reportRepository, new MemberCommandService(memberRepository),
			new MemberQueryService(memberRepository));

		service.beginWithdrawal(member.getId(), member.getEmail());
		service.deleteAll(member.getId(), member.getEmail());
		entityManager.flush();
		entityManager.clear();

		assertThat(feedbackRepository.count()).isZero();
		assertThat(messageRepository.count()).isZero();
		assertThat(chatRepository.count()).isZero();
		assertThat(summaryRepository.count()).isZero();
		assertThat(summaryDeadLetterRepository.count()).isZero();
		assertThat(linkRepository.count()).isZero();
		assertThat(reportRepository.count()).isZero();
		assertThat(memberRepository.findById(member.getId())).isEmpty();
	}
}
