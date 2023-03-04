package com.codestates.mainproject.oneyearfourcut.domain.artwork.service;

import com.codestates.mainproject.oneyearfourcut.domain.alarm.event.AlarmEventPublisher;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.dto.ArtworkPatchDto;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.dto.ArtworkPostDto;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.dto.ArtworkResponseDto;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.dto.OneYearFourCutResponseDto;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.repository.ArtworkRepository;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.service.GalleryService;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.global.aws.service.AwsS3Service;
import com.codestates.mainproject.oneyearfourcut.global.exception.exception.BusinessLogicException;
import com.codestates.mainproject.oneyearfourcut.global.exception.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.data.domain.Sort.Order.desc;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ArtworkService {
    private final ArtworkRepository artworkRepository;
    private final GalleryService galleryService;
    private final AwsS3Service awsS3Service;
    private final AlarmEventPublisher alarmEventPublisher;

    public Artwork findArtwork(long artworkId) {
        Optional<Artwork> artworkOptional = artworkRepository.findById(artworkId);

        return artworkOptional.orElseThrow(
                () -> new BusinessLogicException(ExceptionCode.ARTWORK_NOT_FOUND));
    }

    public Artwork findGalleryVerifiedArtwork(long galleryId, long artworkId) {
        Artwork foundArtwork = findArtwork(artworkId);

        if (!foundArtwork.isInOpenGallery()) {
            throw new BusinessLogicException(ExceptionCode.CLOSED_GALLERY);
        }

        if (!foundArtwork.isCorrectGallery(galleryId)) {
            throw new BusinessLogicException(ExceptionCode.ARTWORK_NOT_FOUND_FROM_GALLERY);
        }


        return foundArtwork;
    }

    public void checkGalleryArtworkVerification(Long galleryId, Long artworkId) {
        Optional<Artwork> artwork = artworkRepository.findById(artworkId);
        Artwork foundArtwork = artwork.orElseThrow(
                () -> new BusinessLogicException(ExceptionCode.ARTWORK_NOT_FOUND));

        Gallery gallery = foundArtwork.getGallery();
        if (galleryId != gallery.getGalleryId()) {
            throw new BusinessLogicException(ExceptionCode.ARTWORK_NOT_FOUND_FROM_GALLERY);
        }
        if (gallery.getStatus() == GalleryStatus.CLOSED) {
            throw new BusinessLogicException(ExceptionCode.CLOSED_GALLERY);
        }
    }

    public List<Artwork> findArtworkList(long galleryId) {
        galleryService.verifiedGalleryExist(galleryId);

        List<Artwork> artworkList = artworkRepository.findAllByGallery_GalleryId(galleryId,
                Sort.by(desc("createdAt")));

        return artworkList;
    }

    //==============================Artwork Controller 용 CRUD==============================//
    @Transactional
    public ArtworkResponseDto createArtwork(long memberId, long galleryId, ArtworkPostDto requestDto) {
        Gallery findGallery = galleryService.findGallery(galleryId);
        Artwork artwork = requestDto.toEntity();
        // 이미지 유효성(null) 검증
        if (artwork.getImage() == null) {
            throw new BusinessLogicException(ExceptionCode.IMAGE_NOT_FOUND_FROM_REQUEST);
        }
        String imageRoot = awsS3Service.uploadFile(artwork.getImage());

        artwork.setGallery(new Gallery(galleryId));
        artwork.setMember(new Member(memberId));
        artwork.setImagePath(imageRoot);

        Artwork savedArtwork = artworkRepository.save(artwork);

        //알람 생성
        Long receiverId = findGallery.getMember().getMemberId();
        alarmEventPublisher.publishAlarmEvent(savedArtwork.toAlarmEvent(receiverId));

        return savedArtwork.toArtworkResponseDto();
    }

    public ArtworkResponseDto findArtworkResponseDto(long memberId, long galleryId, long artworkId) {
        Artwork foundArtwork = findGalleryVerifiedArtwork(galleryId, artworkId);

        foundArtwork.setMemberLike(memberId);
        return foundArtwork.toArtworkResponseDto();
    }

    public List<ArtworkResponseDto> findArtworkResponseDtoList(long memberId, long galleryId) {
        List<Artwork> artworkList = findArtworkList(galleryId);

        if (memberId != -1) {
            artworkList.stream()
                    .forEach(artwork -> artwork.setMemberLike(memberId));
        }

        return artworkList.stream()
                .map(Artwork::toArtworkResponseDto)
                .collect(Collectors.toList());
    }
    public List<OneYearFourCutResponseDto> findOneYearFourCut(long galleryId) {
        galleryService.verifiedGalleryExist(galleryId);

        List<Artwork> findArtworkList = artworkRepository.findTop4ByGallery_GalleryId(galleryId,
                Sort.by(desc("likeCount"), desc("createdAt")));

        return findArtworkList.stream()
                .map(Artwork::toOneYearFourCutResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ArtworkResponseDto updateArtwork(long memberId, long galleryId, long artworkId, ArtworkPatchDto requestDto) {
        Artwork foundArtwork = findGalleryVerifiedArtwork(galleryId, artworkId);

        if (!foundArtwork.isArtworkOwner(memberId)) {
            throw new BusinessLogicException(ExceptionCode.UNAUTHORIZED);
        }

        Artwork request = requestDto.toEntity();
        Optional<MultipartFile> image = Optional.ofNullable(request.getImage());
        if (image.isPresent()) {
            String s3Path = awsS3Service.uploadFile(image.get());
            awsS3Service.deleteImage(foundArtwork.getImagePath());
            request.setImagePath(s3Path);
        }
        foundArtwork.modify(request);
        return foundArtwork.toArtworkResponseDto();
    }
    @Transactional
    public void deleteArtwork(long memberId, long galleryId, long artworkId) {
        Artwork foundArtwork = findGalleryVerifiedArtwork(galleryId, artworkId);

        if (!foundArtwork.isArtworkOwner(memberId) && !foundArtwork.isGalleryOwner(memberId)) {
            throw new BusinessLogicException(ExceptionCode.UNAUTHORIZED);
        }

        awsS3Service.deleteImage(foundArtwork.getImagePath());
        artworkRepository.delete(foundArtwork);
    }
}

















