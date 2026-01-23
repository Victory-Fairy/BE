package kr.co.victoryfairy.support.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HttpStatus 이외에 커스텀한 Status Code를 주고 싶을때 사용
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    STATUS_901(901, "유효한 토큰이 아닙니다."), STATUS_902(902, "리프래시 토큰이 필요합니다."), STATUS_903(903, "~~~~~~~~~"),
    STATUS_904(904, "~~~~"), STATUS_905(905, "xxxxxx"), STATUS_906(906, "xxxxxx"), STATUS_907(907, "xxxxxx"),
    STATUS_908(908, "xxxxxx"), STATUS_909(909, "xxxxxx"), STATUS_910(910, "xxxxxx"), STATUS_911(911, "xxxxxx");

    private int status;

    private String descKr;

}