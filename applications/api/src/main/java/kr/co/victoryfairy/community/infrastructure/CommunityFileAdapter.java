package kr.co.victoryfairy.community.infrastructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import kr.co.victoryfairy.community.application.CommunityFileReader;
import kr.co.victoryfairy.media.domain.MediaFile;
import kr.co.victoryfairy.media.domain.MediaFileRepository;
import kr.co.victoryfairy.media.infrastructure.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityFileAdapter implements CommunityFileReader {

    private final MediaFileRepository files;

    private final S3PresignedUrlService presignedUrls;

    @Override
    public Set<Long> findExistingIds(List<Long> fileIds) {
        return files.findAllById(fileIds).stream().map(MediaFile::id).collect(Collectors.toSet());
    }

    @Override
    public Map<Long, String> findUrls(List<Long> fileIds) {
        var result = new LinkedHashMap<Long, String>();
        files.findAllById(fileIds).forEach(file -> result.put(file.id(),
            presignedUrls.create(file.path(), file.saveName(), file.ext())));
        return result;
    }

}
