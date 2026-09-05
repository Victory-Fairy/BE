package kr.co.victoryfairy.diary.application;

public interface PartnerDto {
    record PartnerSaveRequest(String name, Long teamId) {}
    record PartnerResponse(String name, Long teamId) {}
}
