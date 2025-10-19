package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.repository.LinkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkQueryService {

	private final LinkRepository linkRepository;

}
