
CREATE TABLE common_code_group (
    group_code  VARCHAR(30) NOT NULL,
    group_name  VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (group_code)
);


CREATE INDEX idx_file_ref_ref_type_ref_id_is_use
    ON file_ref (ref_type, ref_id, is_use);


CREATE TABLE common_code_detail (
    group_code  VARCHAR(30) NOT NULL,
    code        VARCHAR(30) NOT NULL,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    sort_order  INT UNSIGNED NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                             ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (group_code, code),
    CONSTRAINT fk_common_code_detail_group
        FOREIGN KEY (group_code)
        REFERENCES common_code_group (group_code)
        ON DELETE CASCADE
);


CREATE TABLE community_post (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    member_id   BIGINT NOT NULL,
    title       VARCHAR(30) NOT NULL,
    content     VARCHAR(100) NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                             ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6) NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_community_post_member
        FOREIGN KEY (member_id)
        REFERENCES member (id)
        ON DELETE CASCADE
);


CREATE TABLE community_post_file (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    post_id  BIGINT NOT NULL,
    file_id  BIGINT NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_community_post_file_post
        FOREIGN KEY (post_id)
        REFERENCES community_post (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_community_post_file_file
        FOREIGN KEY (file_id)
        REFERENCES file (id)
        ON DELETE CASCADE
);


CREATE TABLE community_comment (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    post_id     BIGINT NOT NULL,
    member_id   BIGINT NOT NULL,
    content     VARCHAR(100) NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                             ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6) NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_community_comment_post
        FOREIGN KEY (post_id)
        REFERENCES community_post (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_community_comment_member
        FOREIGN KEY (member_id)
        REFERENCES member (id)
        ON DELETE CASCADE
);


CREATE TABLE community_post_like (
    post_id     BIGINT NOT NULL,
    member_id   BIGINT NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (post_id, member_id),
    CONSTRAINT fk_community_post_like_post
        FOREIGN KEY (post_id)
        REFERENCES community_post (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_community_post_like_member
        FOREIGN KEY (member_id)
        REFERENCES member (id)
        ON DELETE CASCADE
);


CREATE TABLE community_comment_like (
    comment_id  BIGINT NOT NULL,
    member_id   BIGINT NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (comment_id, member_id),
    CONSTRAINT fk_community_comment_like_comment
        FOREIGN KEY (comment_id)
        REFERENCES community_comment (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_community_comment_like_member
        FOREIGN KEY (member_id)
        REFERENCES member (id)
        ON DELETE CASCADE
);


CREATE TABLE community_post_report (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    post_id             BIGINT NOT NULL,
    reporter_id         BIGINT NOT NULL,
    reported_member_id  BIGINT NOT NULL,
    reason_code         VARCHAR(30) NOT NULL,
    status_code         VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    detail              TEXT NULL,
    snapshot            JSON NOT NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                 ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_community_post_report (post_id, reporter_id)
);


CREATE TABLE community_comment_report (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    comment_id          BIGINT NOT NULL,
    reporter_id         BIGINT NOT NULL,
    reported_member_id  BIGINT NOT NULL,
    reason_code         VARCHAR(30) NOT NULL,
    status_code         VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    detail              TEXT NULL,
    snapshot            JSON NOT NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                 ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_community_comment_report (comment_id, reporter_id)
);


INSERT INTO common_code_group (
    group_code,
    group_name,
    description
) VALUES
    ('REPORT_REASON', '신고 사유', '게시글 및 댓글 신고 사유'),
    ('REPORT_STATUS', '신고 처리 상태', '관리자의 신고 처리 상태');


INSERT INTO common_code_detail (
    group_code,
    code,
    name,
    sort_order
) VALUES
    ('REPORT_REASON', 'ABUSE', '욕설 및 비방', 1),
    ('REPORT_REASON', 'SPAM', '스팸 및 도배', 2),
    ('REPORT_REASON', 'INAPPROPRIATE', '부적절한 콘텐츠', 3),
    ('REPORT_REASON', 'OTHER', '기타', 4),
    ('REPORT_STATUS', 'PENDING', '대기', 1),
    ('REPORT_STATUS', 'ACCEPTED', '승인', 2),
    ('REPORT_STATUS', 'REJECTED', '거절', 3);
