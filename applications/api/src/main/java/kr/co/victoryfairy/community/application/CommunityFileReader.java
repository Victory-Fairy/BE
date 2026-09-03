package kr.co.victoryfairy.community.application;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CommunityFileReader {

    Set<Long> findExistingIds(List<Long> fileIds);

    Map<Long, String> findUrls(List<Long> fileIds);

}
