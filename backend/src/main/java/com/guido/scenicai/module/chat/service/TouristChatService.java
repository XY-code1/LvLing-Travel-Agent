package com.guido.scenicai.module.chat.service;

import com.guido.scenicai.module.chat.dto.TextChatDTO;
import com.guido.scenicai.module.chat.vo.ChatAnswerVO;
import com.guido.scenicai.module.chat.vo.VisionRecognizeVO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface TouristChatService {

    ChatAnswerVO answerText(TextChatDTO dto);

    SseEmitter streamText(TextChatDTO dto);

    ChatAnswerVO answerVoice(String sessionNo, MultipartFile audio);

    SseEmitter streamVoice(String sessionNo, MultipartFile audio);

    VisionRecognizeVO recognizeImage(String sessionNo, MultipartFile image);
}
