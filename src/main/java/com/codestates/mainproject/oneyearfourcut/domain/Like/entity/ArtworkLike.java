package com.codestates.mainproject.oneyearfourcut.domain.Like.entity;

import com.codestates.mainproject.oneyearfourcut.domain.alarm.entity.AlarmType;
import com.codestates.mainproject.oneyearfourcut.domain.alarm.event.AlarmEvent;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.global.auditable.Auditable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtworkLike extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long artworkLikeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTWORK_ID")
    private Artwork artwork;

    @Enumerated(EnumType.STRING)
    private LikeStatus status = LikeStatus.LIKE;

    @Builder
    public ArtworkLike(Member member, Artwork artwork) {
        setMember(member);
        setArtwork(artwork);
    }

    private void setMember(Member member) {
        if (this.member != null) {
            member.getArtworkLikeList().remove(this);
        }
        this.member = member;
        member.getArtworkLikeList().add(this);
    }

    private void setArtwork(Artwork artwork) {
        if (this.artwork != null) {
            this.artwork.getArtworkLikeList().remove(this);
        }
        this.artwork = artwork;
        artwork.getArtworkLikeList().add(this);
    }

    public void setStatus(LikeStatus status) {
        this.status = status;
    }

    public AlarmEvent toAlarmEvent(Long receiverId) {
        return AlarmEvent.builder()
                .receiverId(receiverId)
                .senderId(this.getMember().getMemberId())
                .alarmType(AlarmType.LIKE_ARTWORK)
                .galleryId(this.getArtwork().getGallery().getGalleryId())
                .artworkId(this.getArtwork().getArtworkId())
                .build();
    }
}
