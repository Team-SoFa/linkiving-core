package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LinkFacade {

	private final LinkService linkService;
	private final LinkCommandService linkCommandService;
	private final LinkQueryService linkQueryService;
}
