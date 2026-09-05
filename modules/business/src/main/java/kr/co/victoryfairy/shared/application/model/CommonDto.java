package kr.co.victoryfairy.shared.application.model;

import java.util.List;

/**
 * 공통 DTO 인터페이스
 * <p>
 * 여러 애플리케이션에서 공통으로 사용하는 DTO 정의
 */
public interface CommonDto {

    /**
     * 이미지 정보 DTO
     */
    record ImageDto(Long id, String path, String saveName, String ext, String url) {
    }

    /**
     * 파트너(동행자) 저장 요청 DTO
     */
    record PartnerSaveRequest(String name, Long teamId) {
    }

    /**
     * 파트너(동행자) 응답 DTO
     */
    record PartnerResponse(String name, Long teamId) {
    }

}
