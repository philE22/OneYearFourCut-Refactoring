package com.codestates.mainproject.oneyearfourcut.domain.gallery.controller;

import com.codestates.mainproject.oneyearfourcut.domain.gallery.dto.GalleryPatchDto;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.dto.GalleryPostResponseDto;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.dto.GalleryRequestDto;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.dto.GalleryResponseDto;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.service.GalleryService;
import com.codestates.mainproject.oneyearfourcut.global.config.auth.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/galleries")
@RequiredArgsConstructor
@Validated
public class GalleryController {
    private final GalleryService galleryService;

    //전시관 등록
    @PostMapping
    public ResponseEntity postGallery(@Valid @RequestBody GalleryRequestDto galleryRequestDto,
                                      @LoginMember Long memberId) {
        GalleryPostResponseDto galleryPostResponseDto = galleryService.createGallery(galleryRequestDto, memberId);

        return new ResponseEntity<>(galleryPostResponseDto, HttpStatus.CREATED);
    }

    //전시관 조회
    @GetMapping("/{gallery-id}")
    public ResponseEntity getGallery(@PathVariable("gallery-id") Long galleryId) {
        GalleryResponseDto galleryResponseDto = galleryService.findGalleryResponseDto(galleryId);

        return new ResponseEntity<>(galleryResponseDto, HttpStatus.OK);
    }

    //전시관 수정
    @PatchMapping("/me")
    public ResponseEntity patchGallery(@Valid @RequestBody GalleryPatchDto galleryPatchDto,
                                       @LoginMember Long memberId) {
        GalleryResponseDto galleryResponseDto = galleryService.modifyGallery(galleryPatchDto, memberId);

        return new ResponseEntity(galleryResponseDto, HttpStatus.OK);
    }

//    전시관 폐쇄
    @DeleteMapping("/me")
    public ResponseEntity deleteGallery(@LoginMember Long memberId) {
        galleryService.deleteGallery(memberId);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

}
