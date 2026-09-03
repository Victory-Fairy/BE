package kr.co.victoryfairy.community.application;

import java.util.List;
import java.util.Map;

public interface CommunityMemberReader {

    boolean exists(Long memberId);

    Map<Long, Author> findAuthors(List<Long> memberIds);

    record Author(Long memberId, String nickname, String profileImageUrl) {
    }

}
