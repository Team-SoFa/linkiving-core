package com.sofa.linkiving.domain.chat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofa.linkiving.domain.link.controller.LinkApi;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController implements LinkApi {
}
