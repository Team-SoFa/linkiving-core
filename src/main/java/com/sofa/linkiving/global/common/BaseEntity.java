package com.sofa.linkiving.global.common;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
	@LastModifiedDate
	@Column(name = "updated_at")
	protected LocalDateTime updatedAt;
	// reserved for future soft delete implementation
	@Column(name = "is_delete")
	protected boolean isDelete = false;

	public boolean isDeleted() {
		return isDelete;
	}

	public void markDeleted() {
		this.isDelete = true;
	}

	public void restore() {
		this.isDelete = false;
	}

}
