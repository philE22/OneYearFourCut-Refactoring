package com.codestates.mainproject.oneyearfourcut.domain.alarm.entity;

import com.codestates.mainproject.oneyearfourcut.domain.alarm.dto.AlarmResponseDto;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.global.auditable.Auditable;
import lombok.*;

import javax.persistence.*;

@Getter
@Entity
@Table(name = "alarm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alarm extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long alarmId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MEMBER_ID")
    private Member member;

    @Column(name = "READ_CHECK")
    private Boolean readCheck;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private AlarmType alarmType;

    @Column
    private Long senderId;

    @Column
    private Long artworkId;

    @Column
    private Long galleryId;

    @Builder
    public Alarm(Member member, Boolean readCheck, AlarmType alarmType, Long senderId, Long artworkId, Long galleryId) {
        this.member = member;
        this.readCheck = readCheck;
        this.alarmType = alarmType;
        this.senderId = senderId;
        this.artworkId = artworkId;
        this.galleryId = galleryId;
    }

    public AlarmResponseDto toAlarmResponseDto(String nickname, String title) {
        return AlarmResponseDto.builder()
                .alarmId(this.alarmId)
                .alarmType(String.valueOf(this.getAlarmType()))
                .createdAt(this.getCreatedAt())
                .read(this.getReadCheck())
                .galleryId(this.galleryId)
                .artworkId(this.artworkId)
                .userNickname(nickname)
                .artworkTitle(title)
                .build();
    }
    public void checkRead() {
        this.readCheck = true;
    }
}
