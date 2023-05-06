package com.team2.songgpt.controller;

import com.team2.songgpt.dto.gpt.AnswerResponseDto;
import com.team2.songgpt.dto.gpt.CheckModelResponseDto;
import com.team2.songgpt.dto.gpt.GptResponseDto;
import com.team2.songgpt.dto.gpt.QuestionRequestDto;
import com.team2.songgpt.global.dto.ResponseDto;
import com.team2.songgpt.service.GptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("chat-gpt")
@RequiredArgsConstructor
public class GptController {
    private final GptService gptService;
    @PostMapping("/question")
    public ResponseDto<AnswerResponseDto> sendQuestion(@RequestBody QuestionRequestDto requestDto) {
        return gptService.askQuestion(requestDto);
    }

    @PostMapping("/question/text")
    public ResponseDto<AnswerResponseDto> sendTextQuestion(@RequestBody QuestionRequestDto requestDto) {
        return gptService.askTextQuestion(requestDto);
    }

}
