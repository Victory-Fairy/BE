package kr.co.victoryfairy.core.api.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;

public interface DiaryDomain {

    @ParameterObject
    @Schema(name = "Diary.DiaryDto")
    record DiaryDto(
            // 필수 입력값
            @Schema(description = "응원팀", example = "키움")
            String teamName,                    // 응원팀

            @Schema(description = "관람 방식", example = "직관")
            String viewType,                    // 관람 방식

            @Schema(description = "이미지 링크", example = "img_url")
            String imgUrl,                      // 사진 url

            @Schema(description = "경기 식별자", example = "20240309HTNC0")
            String gameMatchId,                 // 경기 식별자

            // 선택 입력값
            @Schema(description = "날씨", example = "흐림")
            String weather,                     // 날씨

            @Schema(description = "좌석 정보", example = "SeatUseHistoryDto")
            SeatUseHistoryDto seat,             // 좌석

            @Schema(description = "음식 리스트", example = "[치킨,맥주]")
            List<String> foodNameList,          // 음식 리스트

            @Schema(description = "함께한 사람 리스트", example = "[PartnerDto]")
            List<PartnerDto> partnerList,       // 함께한 사람 리스트

            @Schema(description = "기분 리스트", example = "[좋음]")
            List<String> moodList               // 기분 리스트
    ) {}

    @ParameterObject
    record PartnerDto(
            @Schema(description = "함께한 사람 이름", example = "홍길동")
            String name,

            @Schema(description = "함께한 사람의 응원 팀", example = "키움")
            String teamName
    ) {}

    @ParameterObject
    record SeatUseHistoryDto(
            @Schema(description = "좌석 식별자", example = "1")
            Long seatId,           // 좌석 식별자

            @Schema(description = "좌석 블록", example = "217")
            String seatBlock,      // 좌석 블록

            @Schema(description = "좌석 열", example = "16")
            String seatRow,        // 좌석 열

            @Schema(description = "좌석 번호", example = "240")
            String seatNumber,     // 좌석 번호

            @Schema(description = "좌석 리뷰", example = "[탁 트인 시야, 넓은 공간]")
            List<String> seatReview // 좌석 리뷰
    ) {
    }

}
