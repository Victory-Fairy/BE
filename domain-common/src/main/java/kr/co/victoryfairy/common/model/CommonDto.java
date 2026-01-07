package kr.co.victoryfairy.common.model;

import java.util.List;

/**
 * 공통 DTO 인터페이스
 * <p>
 * core-api, core-admin 등 여러 모듈에서 공통으로 사용하는 DTO 정의
 */
public interface CommonDto {

    /**
     * 이미지 정보 DTO
     */
    record ImageDto(
            Long id,
            String path,
            String saveName,
            String ext
    ) {}

    /**
     * 파트너(동행자) 저장 요청 DTO
     */
    record PartnerSaveRequest(
            String name,
            Long teamId
    ) {}

    /**
     * 파트너(동행자) 응답 DTO
     */
    record PartnerResponse(
            String name,
            Long teamId
    ) {}
}