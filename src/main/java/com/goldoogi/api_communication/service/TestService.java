package com.goldoogi.api_communication.service;

import org.springframework.http.ResponseEntity;

import com.goldoogi.api_communication.dto.request.board.DCPostRequestDto;
import com.goldoogi.api_communication.dto.response.board.DCPostResponseDto;

public interface TestService {
    ResponseEntity<? super DCPostResponseDto> postBoard(DCPostRequestDto dto);
}
