package com.sofa.linkiving;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.sofa.linkiving.domain.member.config.MemberWithdrawalProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties(MemberWithdrawalProperties.class)
public class LinkivingApplication {

	public static void main(String[] args) {
		SpringApplication.run(LinkivingApplication.class, args);
	}

}
