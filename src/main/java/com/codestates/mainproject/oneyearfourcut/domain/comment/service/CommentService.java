package com.codestates.mainproject.oneyearfourcut.domain.comment.service;

import com.codestates.mainproject.oneyearfourcut.domain.alarm.event.AlarmEventPublisher;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.service.ArtworkService;
import com.codestates.mainproject.oneyearfourcut.domain.comment.dto.CommentArtworkResDto;
import com.codestates.mainproject.oneyearfourcut.domain.comment.dto.CommentGalleryResDto;
import com.codestates.mainproject.oneyearfourcut.domain.comment.dto.CommentRequestDto;
import com.codestates.mainproject.oneyearfourcut.domain.comment.entity.Comment;
import com.codestates.mainproject.oneyearfourcut.domain.comment.repository.CommentRepository;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.service.GalleryService;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.domain.member.service.MemberService;
import com.codestates.mainproject.oneyearfourcut.global.exception.exception.BusinessLogicException;
import com.codestates.mainproject.oneyearfourcut.global.exception.exception.ExceptionCode;
import com.codestates.mainproject.oneyearfourcut.global.page.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final MemberService memberService;
    private final GalleryService galleryService;
    private final ArtworkService artworkService;
    private final AlarmEventPublisher alarmEventPublisher;

    @Transactional
    public CommentGalleryHeadDto<Object> createCommentOnGallery(CommentRequestDto commentRequestDto,
                                                                Long galleryId, Long memberId) {
        Member member = memberService.findMember(memberId);
        Gallery gallery = galleryService.findGallery(galleryId);
        Comment savedComment =
                commentRepository.save(commentRequestDto.toCommentEntity(member, gallery));

        //알림 생성
        Long receiverId = gallery.getMember().getMemberId();
        alarmEventPublisher.publishAlarmEvent(savedComment.toAlarmEvent(receiverId));

        return new CommentGalleryHeadDto<>(galleryId, savedComment.toCommentGalleryResponseDto());
    }

    @Transactional
    public CommentArtworkHeadDto<Object> createCommentOnArtwork(CommentRequestDto commentRequestDto,
                                                                Long galleryId, Long artworkId, Long memberId) {
        Member member = memberService.findMember(memberId);
        Gallery gallery = galleryService.findGallery(galleryId);
        Artwork artwork = artworkService.findArtwork(artworkId);
        Comment savedComment = commentRepository.save(
                commentRequestDto.toCommentEntity(member, gallery, artwork));

        //전시관 주인에게 알림 생성
        Long galleryReceiverId = gallery.getMember().getMemberId();
        alarmEventPublisher.publishAlarmEvent(savedComment.toAlarmEvent(galleryReceiverId));
        //작품 주인에게 알림 생성
        Long artworkReceiverId = artwork.getMember().getMemberId();
        if (artworkReceiverId != galleryReceiverId) {   //자기 전시관에 단 작품이면 알람이 한 번만 오도록 처리
            alarmEventPublisher.publishAlarmEvent(savedComment.toAlarmEvent(artworkReceiverId));
        }

        return new CommentArtworkHeadDto<>(galleryId, artworkId, savedComment.toCommentArtworkResponseDto());
    }

    public CommentGalleryPageResponseDto<Object> getGalleryCommentPage(Long galleryId, int page, int size) {
        galleryService.verifiedGalleryExist(galleryId);
        Page<Comment> commentPage = commentRepository.findAllByGallery_GalleryIdOrderByCommentIdDesc(
                galleryId, PageRequest.of(page - 1, size));

        List<CommentGalleryResDto> response =
                commentPage.getContent().stream()
                        .map(Comment::toCommentGalleryResponseDto)
                        .collect(Collectors.toList());
        PageInfo<Object> pageInfo = new PageInfo<>(
                page, size, (int) commentPage.getTotalElements(), commentPage.getTotalPages());

        return new CommentGalleryPageResponseDto<>(galleryId, response, pageInfo);
    }

    public CommentArtworkPageResponseDto<Object> getArtworkCommentPage(Long galleryId, Long artworkId,
                                                                       int page, int size) {
        artworkService.checkGalleryArtworkVerification(galleryId, artworkId);
        Page<Comment> commentPage = commentRepository.findAllByArtwork_ArtworkIdOrderByCommentIdDesc(
                artworkId, PageRequest.of(page - 1, size));

        List<CommentArtworkResDto> response = commentPage.getContent().stream()
                .map(Comment::toCommentArtworkResponseDto)
                .collect(Collectors.toList());
        PageInfo<Object> pageInfo = new PageInfo<>(
                page, size, (int) commentPage.getTotalElements(), commentPage.getTotalPages());

        return new CommentArtworkPageResponseDto<>(galleryId, artworkId, response, pageInfo);
    }

    @Transactional
    public CommentGalleryHeadDto<Object> modifyComment(Long galleryId, Long commentId,
                                                       CommentRequestDto commentRequestDto, Long memberId) {
        Comment foundComment = findGalleryVerifiedComment(galleryId, commentId);
        if (!foundComment.isOwner(memberId)) {
            throw new BusinessLogicException(ExceptionCode.UNAUTHORIZED);
        }
        Optional.ofNullable(commentRequestDto.getContent())
                .ifPresent(foundComment::changeContent);
        return new CommentGalleryHeadDto<>(galleryId, foundComment.toCommentGalleryResponseDto());
    }

    @Transactional
    public void deleteComment(Long galleryId, Long commentId, Long memberId) {
        Comment foundComment = findGalleryVerifiedComment(galleryId, commentId);
        if (!foundComment.isOwner(memberId) && !foundComment.getGallery().isOwner(memberId)) {
            throw new BusinessLogicException(ExceptionCode.UNAUTHORIZED);
        }

        commentRepository.delete(foundComment);
    }

    public Comment findComment(Long commentId) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        Comment foundComment = comment.orElseThrow(
                () -> new BusinessLogicException(ExceptionCode.COMMENT_NOT_FOUND));
        return foundComment;
    }

    public Comment findGalleryVerifiedComment(Long galleryId, Long commentId) {
        Comment foundComment = findComment(commentId);
        galleryService.verifiedGalleryExist(galleryId);
        if (!Objects.equals(galleryId, foundComment.getGallery().getGalleryId())) {
            throw new BusinessLogicException(ExceptionCode.COMMENT_NOT_FOUND_FROM_GALLERY);
        }

        return foundComment;
    }
}
