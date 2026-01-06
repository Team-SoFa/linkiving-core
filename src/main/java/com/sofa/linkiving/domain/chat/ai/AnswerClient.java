package com.sofa.linkiving.domain.chat.ai;

import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq;
import com.sofa.linkiving.domain.chat.dto.response.RagAnswerRes;

public interface AnswerClient {
	RagAnswerRes generateAnswer(RagAnswerReq request);
}
