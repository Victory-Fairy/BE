
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `last_connect_at` timestamp NULL DEFAULT NULL COMMENT '마지막 접속 일시',
  `last_connect_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '마지막 접속 아이피',
  `admin_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `pwd` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `memo` varchar(255) DEFAULT NULL,
  `team_name` varchar(255) DEFAULT NULL,
  `view_type` enum('HOME','STADIUM') DEFAULT NULL,
  `weather` enum('RAIN','CLEARING','CLOUDY','CLEARING_CLOUD','SUNNY') DEFAULT NULL,
  `game_match_id` varchar(255) DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `is_rated` bit(1) DEFAULT b'0',
  `mood` enum('ANGRY','SAD','NATURAL','SURPRISE','HAPPY') DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`id`),
  KEY `FKbyluyva0mxnf5jitf297oxlxd` (`member_id`),
  KEY `FKirrluexdybf7mfafxgfsr80wm` (`team_id`),
  KEY `FKiaekaijumh3i7tgn5tnqwb0cg` (`game_match_id`),
  CONSTRAINT `FKbyluyva0mxnf5jitf297oxlxd` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKiaekaijumh3i7tgn5tnqwb0cg` FOREIGN KEY (`game_match_id`) REFERENCES `game_match` (`id`),
  CONSTRAINT `FKirrluexdybf7mfafxgfsr80wm` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diary_food` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `food_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `ref_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '참조 구분',
  `ref_id` bigint DEFAULT NULL COMMENT '참조 ID',
  PRIMARY KEY (`id`),
  KEY `idx_diary_food_ref` (`ref_type`,`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `ext` varchar(20) DEFAULT NULL COMMENT '확장자',
  `name` text COMMENT '파일 오리진 이름',
  `path` varchar(50) DEFAULT NULL COMMENT '파일 경로',
  `save_name` varchar(50) DEFAULT NULL COMMENT '파일 저장 이름',
  `size` bigint DEFAULT NULL COMMENT 'Size',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_ref` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `ref_id` bigint DEFAULT NULL COMMENT '참조 ID',
  `ref_type` enum('PROFILE','DIARY') DEFAULT NULL COMMENT '참조 구분',
  `file_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK74acxuvm7jn2j73whxcgnou5l` (`file_id`),
  CONSTRAINT `FK74acxuvm7jn2j73whxcgnou5l` FOREIGN KEY (`file_id`) REFERENCES `file` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `free_diary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `match_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `home_team_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `away_team_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `home_score` smallint DEFAULT NULL,
  `away_score` smallint DEFAULT NULL,
  `stadium_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `match_at` datetime NOT NULL,
  `team_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `view_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mood` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `weather` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `seat_review` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_free_diary_member` (`member_id`),
  CONSTRAINT `fk_free_diary_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_match` (
  `id` varchar(255) NOT NULL,
  `league` varchar(10) NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `is_use` bit(1) DEFAULT NULL COMMENT '사용 여부',
  `is_send_push` bit(1) DEFAULT b'1' COMMENT '사용 여부',
  `away_nm` varchar(255) DEFAULT NULL COMMENT '어웨이 팀명',
  `away_score` smallint DEFAULT NULL COMMENT '어웨이 점수',
  `home_nm` varchar(255) DEFAULT NULL COMMENT '홈 팀명',
  `home_score` smallint DEFAULT NULL COMMENT '홈 스코어',
  `match_at` datetime(6) DEFAULT NULL COMMENT '경기 일자',
  `reason` varchar(255) DEFAULT NULL COMMENT '사유',
  `season` varchar(255) DEFAULT NULL COMMENT '시즌',
  `stadium` varchar(255) DEFAULT NULL COMMENT '경기장',
  `status` enum('READY','PROGRESS','END','CANCELED') DEFAULT NULL COMMENT '경기 상태',
  `type` enum('EXHIBITION','REGULAR','POST','TIEBREAKER') DEFAULT NULL COMMENT '경기 타입',
  `away_id` bigint DEFAULT NULL COMMENT '어웨이',
  `home_id` bigint DEFAULT NULL COMMENT '홈',
  `updated_at` timestamp NULL DEFAULT NULL,
  `series` enum('EXHIBITION','REGULAR','WILDCARD','TIEBREAKER','SEMI_PLAYOFF','PLAYOFF','KOREA') DEFAULT NULL COMMENT '시리즈 타입',
  `is_match_info_craw` bit(1) DEFAULT NULL COMMENT '경기 내용 크롤링 여부',
  `stadium_id` bigint DEFAULT NULL COMMENT '경기장',
  PRIMARY KEY (`id`),
  KEY `FK30g3sp99a7rnmgwl226rk4wi5` (`away_id`),
  KEY `FKp4x0ao0e6fcpal9kt7rmn6y9d` (`home_id`),
  KEY `FKi2tm3ewogmwvsw7x8s8k8xdgf` (`stadium_id`),
  KEY `idx_game_match_league` (`league`),
  KEY `idx_game_match_league_season` (`league`,`season`),
  CONSTRAINT `FK30g3sp99a7rnmgwl226rk4wi5` FOREIGN KEY (`away_id`) REFERENCES `team` (`id`),
  CONSTRAINT `FKi2tm3ewogmwvsw7x8s8k8xdgf` FOREIGN KEY (`stadium_id`) REFERENCES `stadium` (`id`),
  CONSTRAINT `FKp4x0ao0e6fcpal9kt7rmn6y9d` FOREIGN KEY (`home_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `opponent_team_id` bigint DEFAULT NULL COMMENT '상대 팀 id',
  `team_id` bigint DEFAULT NULL COMMENT '응원 팀 id',
  `opponent_team_name` varchar(255) DEFAULT NULL,
  `team_name` varchar(255) DEFAULT NULL,
  `status` enum('READY','PROGRESS','END','CANCELED') DEFAULT NULL COMMENT '경기 상태',
  `result_type` enum('WIN','LOSS','DRAW') DEFAULT NULL COMMENT '경기 결과',
  `game_match_id` varchar(255) DEFAULT NULL,
  `view_type` enum('HOME','STADIUM') DEFAULT NULL COMMENT '관람 타입',
  `diary_id` bigint DEFAULT NULL,
  `stadium_id` bigint DEFAULT NULL COMMENT '경기장',
  `season` varchar(255) DEFAULT NULL,
  `league_type` varchar(10) DEFAULT 'KBO',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_game_record_diary_id` (`diary_id`),
  KEY `FKdlngbedp70ggchw2e0dao3sqd` (`member_id`),
  KEY `FKngclhkq3j1dsug7yp7rwhxuqh` (`opponent_team_id`),
  KEY `FKqjfv4mu1xku4lqjgsypaswl6b` (`team_id`),
  KEY `FK932w87r0lx4prpeuywbt47643` (`diary_id`),
  KEY `FKfajtpsjs72j4m20d14cek0iku` (`stadium_id`),
  KEY `FK1rv18sw7qycuo5yd366nogbww` (`game_match_id`),
  KEY `idx_game_record_league_type` (`league_type`),
  CONSTRAINT `FK1rv18sw7qycuo5yd366nogbww` FOREIGN KEY (`game_match_id`) REFERENCES `game_match` (`id`),
  CONSTRAINT `FK932w87r0lx4prpeuywbt47643` FOREIGN KEY (`diary_id`) REFERENCES `diary` (`id`),
  CONSTRAINT `FKdlngbedp70ggchw2e0dao3sqd` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKfajtpsjs72j4m20d14cek0iku` FOREIGN KEY (`stadium_id`) REFERENCES `stadium` (`id`),
  CONSTRAINT `FKngclhkq3j1dsug7yp7rwhxuqh` FOREIGN KEY (`opponent_team_id`) REFERENCES `team` (`id`),
  CONSTRAINT `FKqjfv4mu1xku4lqjgsypaswl6b` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hitter_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `game_match_id` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL COMMENT '선수명',
  `position` varchar(255) DEFAULT NULL COMMENT '포지션',
  `turn` smallint DEFAULT NULL COMMENT '타순',
  `hit_count` smallint DEFAULT NULL COMMENT '타수',
  `hit_score` smallint DEFAULT NULL COMMENT '타점',
  `home_run` smallint DEFAULT NULL COMMENT '홈런',
  `hit` smallint DEFAULT NULL COMMENT '안타',
  `ball_four` smallint DEFAULT NULL COMMENT '4사구',
  `score` smallint DEFAULT NULL COMMENT '득점',
  `strike_out` smallint DEFAULT NULL COMMENT '삼진',
  `season` varchar(255) DEFAULT NULL COMMENT '시즌',
  `is_home` bit(1) DEFAULT NULL COMMENT '홈/어웨이 여부',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` timestamp NOT NULL COMMENT '생성일시',
  `is_use` bit(1) NOT NULL DEFAULT b'1' COMMENT '사용여부',
  `last_connect_at` timestamp NULL DEFAULT NULL COMMENT '마지막 접속 일시',
  `last_connect_ip` varchar(255) DEFAULT NULL COMMENT '마지막 접속 아이피',
  `status` enum('NORMAL','WITHDRAWAL','CUTOFF') NOT NULL DEFAULT 'NORMAL' COMMENT '회원 상태',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '수정일시',
  `fcm_token` varchar(255) DEFAULT NULL COMMENT 'fcm 토큰',
  `create_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `birth_year` int DEFAULT NULL COMMENT '나이',
  `created_at` timestamp NOT NULL COMMENT '생성일시',
  `email` varchar(40) NOT NULL COMMENT '이메일',
  `nick_nm` varchar(255) DEFAULT NULL COMMENT '닉네임',
  `sex` varchar(1) DEFAULT NULL COMMENT '성별',
  `sns_id` varchar(255) NOT NULL COMMENT 'sns 아이디',
  `sns_type` enum('KAKAO','NAVER','GOOGLE','APPLE') NOT NULL COMMENT '소셜 타입',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '수정일시',
  `member_id` bigint DEFAULT NULL COMMENT '회원 정보 ID',
  `team_id` bigint DEFAULT NULL COMMENT '응원 팀',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_fgbsd108ol4libwh4yyg2hcy6` (`member_id`),
  KEY `FK945g9n71ri7xirxt78govgpgq` (`team_id`),
  CONSTRAINT `FK945g9n71ri7xirxt78govgpgq` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`),
  CONSTRAINT `FKbptteae7bfaa7obi1ohs523m0` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `partner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `team_name` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `team_id` bigint DEFAULT NULL,
  `ref_type` varchar(20) DEFAULT NULL COMMENT '참조 구분',
  `ref_id` bigint DEFAULT NULL COMMENT '참조 ID',
  PRIMARY KEY (`id`),
  KEY `FK2aulvx1mdb269mlbkroc6jmrl` (`team_id`),
  KEY `idx_partner_ref` (`ref_type`,`ref_id`),
  CONSTRAINT `FK2aulvx1mdb269mlbkroc6jmrl` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pitcher_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `game_match_id` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL COMMENT '선수명',
  `position` varchar(255) DEFAULT NULL COMMENT '포지션',
  `turn` smallint DEFAULT NULL COMMENT '순서',
  `inning` varchar(255) DEFAULT NULL COMMENT '이닝',
  `pitching` smallint DEFAULT NULL COMMENT '투구수',
  `strike_out` smallint DEFAULT NULL COMMENT '삼진',
  `score` smallint DEFAULT NULL COMMENT '실점',
  `hit` smallint DEFAULT NULL COMMENT '피안타',
  `ball_four` smallint DEFAULT NULL COMMENT '4사구',
  `home_run` smallint DEFAULT NULL COMMENT '피홈런',
  `season` varchar(255) DEFAULT NULL COMMENT '시즌',
  `is_home` bit(1) DEFAULT NULL COMMENT '홈/어웨이 여부',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `stadium_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `season` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKipjvldlawalyw885kl3hdtka0` (`stadium_id`),
  CONSTRAINT `FKipjvldlawalyw885kl3hdtka0` FOREIGN KEY (`stadium_id`) REFERENCES `stadium` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seat_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seat_review` varchar(255) DEFAULT NULL,
  `seat_use_history_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfeo9ctb69yplgvnf87p5gkbnp` (`seat_use_history_id`),
  CONSTRAINT `FKfeo9ctb69yplgvnf87p5gkbnp` FOREIGN KEY (`seat_use_history_id`) REFERENCES `seat_use_history` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seat_use_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seat_block` varchar(255) DEFAULT NULL,
  `seat_number` smallint DEFAULT NULL,
  `seat_row` varchar(255) DEFAULT NULL,
  `diary_id` bigint DEFAULT NULL,
  `seat_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `seat_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqv57rtckasjytnpe7h7fonled` (`seat_id`),
  KEY `FKglfluq16432gpyfbxhcnmm606` (`diary_id`),
  CONSTRAINT `FKglfluq16432gpyfbxhcnmm606` FOREIGN KEY (`diary_id`) REFERENCES `diary` (`id`),
  CONSTRAINT `FKqv57rtckasjytnpe7h7fonled` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stadium` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `region` varchar(255) DEFAULT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `short_name` varchar(255) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `external_id` int DEFAULT NULL COMMENT 'MLB Venue ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL COMMENT '팀명',
  `kbo_nm` varchar(255) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `sponsor_nm` varchar(255) DEFAULT NULL COMMENT '스폰서 명',
  `order_no` smallint DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1' COMMENT '사용여부',
  `updated_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `league` varchar(10) DEFAULT 'KBO',
  `country_code` varchar(3) DEFAULT NULL,
  `mlb_team_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_team_league` (`league`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `winning_rate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `home_avg` float DEFAULT NULL COMMENT '집관 승률',
  `home_cnt` smallint DEFAULT NULL COMMENT '집관 기룩 수',
  `home_win_cnt` smallint DEFAULT NULL COMMENT '집관 승 수',
  `season` varchar(255) DEFAULT NULL COMMENT '시즌',
  `stadium_avg` float DEFAULT NULL COMMENT '직관 승률',
  `stadium_cnt` smallint DEFAULT NULL COMMENT '직관 기록 수',
  `stadium_win_cnt` smallint DEFAULT NULL COMMENT '직관 승 수',
  `total_avg` float DEFAULT NULL COMMENT '전체 승률',
  `total_cnt` smallint DEFAULT NULL COMMENT '시즌 총 기록 수',
  `total_win_cnt` smallint DEFAULT NULL COMMENT '전체 승 수',
  `member_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKx9t6uxke89i4d765nnhh3k3b` (`member_id`),
  CONSTRAINT `FKx9t6uxke89i4d765nnhh3k3b` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `withdrawal_reason` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_use` bit(1) NOT NULL DEFAULT b'1',
  `updated_at` datetime(6) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

