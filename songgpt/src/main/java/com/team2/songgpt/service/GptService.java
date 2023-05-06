package com.team2.songgpt.service;

import com.team2.songgpt.dto.gpt.*;
import com.team2.songgpt.global.config.GptConfig;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.internal.IgnoreForbiddenApisErrors;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GptService {

    private final GptConfig gptConfig;
    private static RestTemplate restTemplate = new RestTemplate();

    public HttpEntity<GptRequestDto> buildHttpEntity(GptRequestDto requestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(GptConfig.MEDIA_TYPE));
        headers.add(GptConfig.AUTHORIZATION, GptConfig.BEARER + gptConfig.getApiKey());
        return new HttpEntity<>(requestDto, headers);
    }

    public CheckModelResponseDto checkModel(){
        HttpHeaders headers = new HttpHeaders();
        headers.add(GptConfig.AUTHORIZATION, GptConfig.BEARER + gptConfig.getApiKey());
        CheckModelRequestDto checkModelRequestDto = new CheckModelRequestDto();
        return this.getModelInfo(new HttpEntity<>(checkModelRequestDto, headers));
    }

    public GptResponseDto getResponse(HttpEntity<GptRequestDto> chatGptRequestDtoHttpEntity) {
        ResponseEntity<GptResponseDto> responseEntity = restTemplate.postForEntity(
                GptConfig.URL,
                chatGptRequestDtoHttpEntity,
                GptResponseDto.class);

        return responseEntity.getBody();
    }

    public CheckModelResponseDto getModelInfo(HttpEntity<CheckModelRequestDto> httpEntity) {
        ResponseEntity<CheckModelResponseDto> responseEntity = restTemplate.postForEntity(
                GptConfig.MODEL_INFO_URL+GptConfig.MODEL,
                httpEntity,
                CheckModelResponseDto.class);

        System.out.println(GptConfig.MODEL_INFO_URL+GptConfig.MODEL);
        return responseEntity.getBody();
    }


    public GptResponseDto askQuestion(QuestionRequestDto requestDto) {
        List<Messages> messages = new ArrayList<>();
        messages.add(new Messages(requestDto.getQuestion(), "user"));

        return this.getResponse(this.buildHttpEntity(new GptRequestDto(GptConfig.MODEL, messages)));
    }
}
