package com.goldoogi.api_communication.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goldoogi.api_communication.service.DCPostService;
import com.goldoogi.api_communication.service.TestService;

import jakarta.validation.Valid;

import com.goldoogi.api_communication.dto.request.board.DCPostRequestDto;
import com.goldoogi.api_communication.dto.response.board.DCPostResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/DCPost")
@RequiredArgsConstructor
public class DCPostController {
    
    private final DCPostService dcPostService;

    @PostMapping("")
    public ResponseEntity<? super DCPostResponseDto> postBoard(
        @RequestBody @Valid DCPostRequestDto requestBody
    ) {
        ResponseEntity<? super DCPostResponseDto> response = dcPostService.postBoard(requestBody);
        return response;
    }    
}
