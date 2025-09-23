package com.sofa.linkiving.domain.link.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.link.service.LinkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

	private final LinkService linkService;

	// TODO: API 엔드포인트 추가 예정
}
