package kr.co.victoryfairy.core.event.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.event.model.EventDomain;
import kr.co.victoryfairy.storage.db.core.model.MemberModel;
import kr.co.victoryfairy.storage.db.core.repository.MemberCustomRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class PushService {

    private final Logger log = LoggerFactory.getLogger(PushService.class);

    private final MemberCustomRepository memberCustomRepository;

    private final TeamRepository teamRepository;

    public PushService(MemberCustomRepository memberCustomRepository, TeamRepository teamRepository) {
        this.memberCustomRepository = memberCustomRepository;
        this.teamRepository = teamRepository;
    }

    public boolean processPushFcm(EventDomain.PushEventDto pushEventDto) {
        log.info(">>> Start processing push event: {}", pushEventDto.gameId());

        var awayTeamEntity = teamRepository.findById(pushEventDto.awayId()).orElse(null);

        var homeTeamEntity = teamRepository.findById(pushEventDto.homeId()).orElse(null);

        var memberList = memberCustomRepository.findFcmTokenByTeamId(pushEventDto.awayId(), pushEventDto.homeId());

        if (memberList.isEmpty() || awayTeamEntity == null || homeTeamEntity == null) {
            return false;
        }

        List<String> fcmList = memberList.stream()
            .filter(m -> StringUtils.hasText(m.getFcmToken()))
            .map(s -> s.getFcmToken())
            .toList();

        MulticastMessage messages = MulticastMessage.builder()
            .addAllTokens(fcmList)
            .setNotification(Notification.builder()
                .setTitle("승요의 일기장")
                .setBody(pushEventDto.status().equals(MatchEnum.MatchStatus.PROGRESS) ? "야구 볼 시간이에요⚾️"
                        : "오늘 경기는 취소 되었어요⚾️")
                .build())
            .build();

        try {
            FirebaseMessaging.getInstance().sendEachForMulticast(messages);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
