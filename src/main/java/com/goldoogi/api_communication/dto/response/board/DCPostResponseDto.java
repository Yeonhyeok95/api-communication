package com.goldoogi.api_communication.dto.response.board;

import lombok.Getter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.goldoogi.api_communication.common.ResponseCode;
import com.goldoogi.api_communication.common.ResponseMessage;
import com.goldoogi.api_communication.dto.response.ResponseDto;

@Getter
public class DCPostResponseDto extends ResponseDto {
    
    public DCPostResponseDto() {
        super(ResponseCode.SUCCESS, ResponseMessage.SUCCESS);
    }

    public static ResponseEntity<DCPostResponseDto> success() {
        DCPostResponseDto result = new DCPostResponseDto();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    } 

    public static ResponseEntity<ResponseDto> notExistUser() {
        ResponseDto result = new ResponseDto(ResponseCode.NOT_EXISTED_USER, ResponseMessage.NOT_EXISTED_USER);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    public static ResponseEntity<ResponseDto> notServerWorking() {
        ResponseDto result = new ResponseDto(ResponseCode.SERVER_UNAVAILABLE, ResponseMessage.SERVER_UNAVAILABLE);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }
}
