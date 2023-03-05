package com.codestates.mainproject.oneyearfourcut.domain.artwork.entity;


import com.codestates.mainproject.oneyearfourcut.domain.Like.entity.ArtworkLike;
import com.codestates.mainproject.oneyearfourcut.domain.alarm.entity.AlarmType;
import com.codestates.mainproject.oneyearfourcut.domain.alarm.event.AlarmEvent;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.dto.ArtworkResponseDto;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.dto.OneYearFourCutResponseDto;
import com.codestates.mainproject.oneyearfourcut.domain.comment.entity.Comment;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.global.auditable.Auditable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artwork extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long artworkId;

    @Column(length = 20, nullable = false)
    private String title;

    @Column(length = 70, nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String imagePath;

    @Transient
    private MultipartFile image;

    @Formula("(select count(*) from artwork_like v where v.artwork_id = artwork_id and v.status = 'LIKE')")
    private int likeCount;
    @Transient
    private boolean liked;

    @Formula("(select count(*) from comment c where c.artwork_id = artwork_id)")
    private int commentCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GALLERY_ID")
    private Gallery gallery;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private Member member;

    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtworkLike> artworkLikeList = new ArrayList<>();

    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> commentList = new ArrayList<>();

    public void modify(Artwork artwork) {
        Optional.ofNullable(artwork.getImagePath())
                .ifPresent(imagePath -> this.imagePath = imagePath);
        Optional.ofNullable(artwork.getTitle())
                .ifPresent(title -> this.title = title);
        Optional.ofNullable(artwork.getContent())
                .ifPresent(content -> this.content = content);
    }
    public boolean isInOpenGallery() {
        return this.getGallery().getStatus() == GalleryStatus.OPEN;
    }
    public boolean isCorrectGallery(long galleryId) {
        return this.getGallery().getGalleryId() == galleryId;
    }
    public void setMemberLike(long memberId) {
        if (memberId != -1) {
            this.getArtworkLikeList().stream()
                    .forEach(artworkLike -> {
                        if (artworkLike.getMember().getMemberId() == memberId) {
                            this.liked = true;
                        }
                    });
        }
    }

    public boolean isArtworkOwner(long artworkMemberId) {
        return this.getMember().getMemberId() == artworkMemberId;
    }
    public boolean isGalleryOwner(long galleryMemberId) {
        return this.getGallery().getMember().getMemberId() == galleryMemberId;
    }

    /* ################### Getter ################### */
    public Long getMemberId() {
        return this.member.getMemberId();
    }

    /* ################### Setter ################### */
    public void setGallery(Gallery gallery) {
        if (this.gallery != null) {
            this.gallery.getArtworkList().remove(this);
        }
        this.gallery = gallery;
        gallery.getArtworkList().add(this);
    }
    public void setMember(Member member) {
        if (this.member != null) {
            this.member.getArtworkList().remove(this);
        }
        this.member = member;
        member.getArtworkList().add(this);
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /* ################### 생성자 ################### */

    @Builder
    public Artwork(String title, String content, MultipartFile image) {
        this.title = title;
        this.content = content;
        this.image = image;
    }
    /* ################### toDto ################### */

    public ArtworkResponseDto toArtworkResponseDto() {
        return ArtworkResponseDto.builder()
                .artworkId(artworkId)
                .memberId(getMemberId())
                .nickName(member.getNickname())
                .title(title)
                .content(content)
                .imagePath(imagePath)
                .likeCount(likeCount)
                .liked(liked)
                .commentCount(commentCount)
                .build();
    }
    public OneYearFourCutResponseDto toOneYearFourCutResponseDto() {
        return OneYearFourCutResponseDto.builder()
                .artworkId(getArtworkId())
                .imagePath(getImagePath())
                .likeCount(getLikeCount())
                .build();
    }

    public AlarmEvent toAlarmEvent(Long receiverId) {
        return AlarmEvent.builder()
                .receiverId(receiverId)
                .senderId(this.getMember().getMemberId())
                .alarmType(AlarmType.POST_ARTWORK)
                .galleryId(this.getGallery().getGalleryId())
                .artworkId(this.getArtworkId())
                .build();
    }
}
