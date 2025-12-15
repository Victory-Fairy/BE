package kr.co.victoryfairy.support.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public interface  MessageEnum {

    @Getter
    @AllArgsConstructor
    enum Common implements EnumDescriptor {
        REQUEST_PARAMETER("파라미터를 확인해주세요.", "Parameter is null"),
        REQUEST("요청이 완료되었습니다.", "Request completed."),
        REQUEST_FAIL("요청이 실패 하였습니다.", "Request failed."),
        MODIFY("수정이 완료되었습니다.", "Modification is complete."),
        SAVE("등록이 완료되었습니다.", "Registration is complete."),
        UPDATE("수정이 완료되었습니다.", "Update is complete."),
        DELETE("삭제가 완료되었습니다.", "Deletion is complete."),
        APPROVE("승인이 완료되었습니다.", "Approval completed."),
        FAIL_APPROVE_FAIL("승인이 취소되었습니다.", "Approval has been revoked.");

        private String descKr;
        private String descEn;
    }

    @Getter
    @AllArgsConstructor
    enum Auth implements EnumDescriptor {

        LOGOUT("로그아웃 되었습니다","You have been logged out."),

        FAIL_LOGIN("로그인에 실패하였습니다.","Login failed."),
        FAIL_CUTOFF("차단된 회원입니다.","blocked member."),
        FAIL_WITHDRAWAL("탈퇴한 회원입니다.","withdrawal member."),
        FAIL_EXPIRE_AUTH("인증이 유효하지 않습니다.","Authentication is not vlid."),
        FAIL_NEED_REFRESH("리프래시 토큰이 필요합니다.","Need a refresh token."),
        FAIL_VALID_TOKEN("유효한 토큰이 아닙니다.","Not a valid token."),
        FAIL_DENY("권한이 존재하지 않습니다..","Permission does not exist."),
        FAIL_VALID_EXPIRED("인증 토큰이 만료되었습니다..","The authentication token has expired."),
        FAIL_OVERLAP("중복된 로그인 입니다.", "Duplicate login."),
        FAIL_VALID_EXPIRED_CERT_NUMBER("인증 번호가 만료되었습니다..","The cert number has expired."),
        ILLEGAL_REDIRECT_URI("잘못된 redirect uri 입니다.","Illegal redirect uri."),
        ILLEGAL_REQUEST("잘못된 request 입니다.", "Illegal request."),
        ILLEGAL_CODE("잘못된 code 입니다.","Illegal code,"),
        ILLEGAL_STATUS("잘못된 state 입니다.","Illegal state."),
        FAIL_SNS("SNS 로그인에 실패했습니다.","failed sns login.");

        private String descKr;
        private String descEn;
    }

    @Getter
    @AllArgsConstructor
    enum Data implements EnumDescriptor {
        FAIL_NO_RESULT("해당 데이터가 존재하지 않습니다.","The result does not exist."),
        FAIL_DUPLICATE("해당 데이터가 이미 등록 되어있습니다.","Data is duplicated."),
        FAIL_NOT_NULL("필수 파라미터가 존재하지 않습니다.","Required parameters not entered."),
        AVAILABLE("사용 가능한 데이터 입니다.","This is the data available."),
        WRONG_APPROACH("잘못된 접근 입니다.","This is wrong approach."),
        NO_INTEREST_TEAM("등록된 응원 팀이 없습니다.", "No interest team found."),
        TRY_AGAIN_MOMENT("잠시 후 다시 시도하세요", "Trying again moment."),
        ;

        private String descKr;
        private String descEn;
    }

    @Getter
    @AllArgsConstructor
    enum Valid implements EnumDescriptor {
        FAIL_EMAIL("Email 형식이 아닙니다.","Not in email format."),
        FAIL_NULL("Null을 허용 하지 않습니다.","Null values are not allowed."),
        FAIL_NOT_FOUND_ENDPOINT("엔드포인트가 존재 하지 않습니다.","Endpoint does not exist."),
        FAIL_VALID_AVAILABLE("해당 데이터는 사용 할 수 없습니다.","That data is not available.");

        private String descKr;
        private String descEn;
    }

    @Getter
    @AllArgsConstructor
    enum CheckNick implements EnumDescriptor {
        AVAILABLE("사용 가능한 닉네임입니다.", "Available nickname."),
        DUPLICATE("중복된 닉네임입니다.", "Duplicated nickname."),
        ;
        private String descKr;
        private String descEn;
    }

    @Getter
    @AllArgsConstructor
    enum File implements EnumDescriptor {

        UPLOAD("업로드가 완료되었습니다.","Upload completed."),
        FAIL_UPLOAD("업로드가 실패하였습니다.","Upload failed."),
        FAIL_UPLOAD_FORMAT("지정된 형식이 아닙니다.","Not in specified format."),
        FAIL_RESIZE("리사이즈에 실패했습니다.", "Fail resize"),

        DOWNLOAD("파일 다운로드를 성공하였습니다.","Download was successful."),
        FAIL_DOWNLOAD("파일 다운로드를 실패하였습니다..","Download failed."),
        FAIL_MAX_SIZE("파일 크기가 허용된 최대 크기를 초과하였습니다.","The file size exceeds the maximum allowed limit."),

        DELETE("파일 삭제가 완료되었습니다.", "File deletion is complete."),

        WRONG_FILE("잘못된 파일입니다.", "Wrong file")
        ;

        private String descKr;
        private String descEn;
    }
}