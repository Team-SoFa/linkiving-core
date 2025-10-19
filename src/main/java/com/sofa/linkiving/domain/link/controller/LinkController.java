package com.sofa.linkiving.domain.link.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.link.service.LinkFacade;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/link")
@RequiredArgsConstructor
public class LinkController implements LinkApi {

	private final LinkFacade linkFacade;

	// TODO: API 엔드포인트 추가 예정
}
