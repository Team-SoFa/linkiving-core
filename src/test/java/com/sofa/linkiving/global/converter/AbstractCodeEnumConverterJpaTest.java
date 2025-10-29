package com.sofa.linkiving.global.converter;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import lombok.Getter;
import lombok.Setter;

@DataJpaTest
@ActiveProfiles("test")
class AbstractCodeEnumConverterJpaTest {

	@PersistenceContext
	EntityManager em;

	@Test
	@DisplayName("숫자 코드 컬럼을 통해 Enum을 저장·조회")
	void shouldPersistAndLoadWithConverter() {
		// given
		TestEnumEntity testEnumEntity = new TestEnumEntity();
		testEnumEntity.setName("hello");
		testEnumEntity.setStatus(TestEnum.B); // code=2

		em.persist(testEnumEntity);
		em.flush();
		em.clear();

		// when
		TestEnumEntity found = em.find(TestEnumEntity.class, testEnumEntity.getId());

		// then
		assertThat(found).isNotNull();
		assertThat(found.getStatus()).isEqualTo(TestEnum.B);
		assertThat(found.getName()).isEqualTo("hello");
	}

	@Entity(name = "TestEntity")
	@Getter
	@Setter
	public static class TestEnumEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@Column(nullable = false)
		private TestEnum status;
	}
}
