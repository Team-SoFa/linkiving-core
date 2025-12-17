package com.sofa.linkiving.domain.chat.dto.internal;

import java.util.List;

import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.link.dto.internal.LinkDto;

public record MessageDto(Message message, List<LinkDto> linkDtos) {
}
