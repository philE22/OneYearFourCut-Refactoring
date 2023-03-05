package com.codestates.mainproject.oneyearfourcut.e2e.like;

import com.codestates.mainproject.oneyearfourcut.domain.Like.entity.ArtworkLike;
import com.codestates.mainproject.oneyearfourcut.domain.Like.entity.LikeStatus;
import com.codestates.mainproject.oneyearfourcut.domain.Like.repository.ArtworkLikeRepository;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.repository.ArtworkRepository;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.repository.GalleryRepository;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.MemberStatus;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Role;
import com.codestates.mainproject.oneyearfourcut.domain.member.repository.MemberRepository;
import com.codestates.mainproject.oneyearfourcut.global.config.auth.jwt.JwtTokenizer;
import com.codestates.mainproject.oneyearfourcut.global.exception.exception.ExceptionCode;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PutArtworkLikeTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Gson gson;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private GalleryRepository galleryRepository;
    @Autowired
    private ArtworkRepository artworkRepository;
    @Autowired
    private ArtworkLikeRepository artworkLikeRepository;
    @Autowired
    private JwtTokenizer jwtTokenizer;

    private Member galleryMember;
    private Member artworkMember;
    private Member likeMember;
    private Gallery savedGallery;
    private Artwork savedArtwork;

    @BeforeEach
    void setUp() {
        galleryMember = memberRepository.save(Member.builder()
                .nickname("gallery Writer")
                .email("gallery@gmail.com")
                .profile("/path/gallery")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        savedGallery = galleryRepository.save(Gallery.builder()
                .title("gallery title")
                .content("gallery content")
                .member(galleryMember)
                .status(GalleryStatus.OPEN)
                .build());

        artworkMember = memberRepository.save(Member.builder()
                .nickname("artwork Writer")
                .email("artwork@gmail.com")
                .profile("/path/artwork")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        Artwork artwork = Artwork.builder()
                .title("artwork title")
                .content("artwork content")
                .build();
        artwork.setImagePath("/path/artwork");
        artwork.setGallery(savedGallery);
        artwork.setMember(artworkMember);
        savedArtwork = artworkRepository.save(artwork);

        likeMember = memberRepository.save(Member.builder()
                .nickname("like Writer")
                .email("like@gmail.com")
                .profile("/path/like")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    @DisplayName("정상적인 등록은 성공한다")
    @Test
    void successPutTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(likeMember);

        //when
        ResultActions actions = mockMvc.perform(
                put("/galleries/{gallery-id}/artworks/{artwork-id}/likes",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId()
                )
                        .header("Authorization", jwt)
        );
        ArtworkLike like = artworkLikeRepository.findByMemberAndArtwork(likeMember, savedArtwork).get();

        //then
        actions.andExpect(status().isOk());
        assertThat(like.getStatus()).isEqualTo(LikeStatus.LIKE);

    }

    @DisplayName("한번 더 누르면 좋아요가 취소된다")
    @Test
    void cancelTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(likeMember);

        ArtworkLike savedLike =
                artworkLikeRepository.save(ArtworkLike.builder()
                        .member(likeMember)
                        .artwork(savedArtwork)
                        .build());

        //when
        ResultActions actions = mockMvc.perform(
                put("/galleries/{gallery-id}/artworks/{artwork-id}/likes",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId()
                )
                        .header("Authorization", jwt)
        );
        ArtworkLike like = artworkLikeRepository.findByMemberAndArtwork(likeMember, savedArtwork).get();

        //then
        actions.andExpect(status().isOk());
        assertThat(like.getStatus()).isEqualTo(LikeStatus.CANCEL);
        assertThat(savedLike.getArtworkLikeId()).isEqualTo(like.getArtworkLikeId());
    }

    @DisplayName("전시회의 path가 다르면 실패한다")
    @Test
    void galleryPathTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(likeMember);
        Gallery anotherGallery = galleryRepository.save(Gallery.builder()
                .title("gallery title")
                .content("gallery content")
                .member(artworkMember)
                .status(GalleryStatus.OPEN)
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                put("/galleries/{gallery-id}/artworks/{artwork-id}/likes",
                        anotherGallery.getGalleryId(),
                        savedArtwork.getArtworkId()
                )
                        .header("Authorization", jwt)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.ARTWORK_NOT_FOUND_FROM_GALLERY.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.ARTWORK_NOT_FOUND_FROM_GALLERY.name()));
    }

    @DisplayName("토큰이 없으면 실패한다")
    @Test
    void authorizationTest() throws Exception {
        //when
        ResultActions actions = mockMvc.perform(
                put("/galleries/{gallery-id}/artworks/{artwork-id}/likes",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId()
                )
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.UNAUTHORIZED.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.UNAUTHORIZED.name()));
    }
}
