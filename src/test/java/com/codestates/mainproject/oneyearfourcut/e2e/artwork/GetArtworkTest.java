package com.codestates.mainproject.oneyearfourcut.e2e.artwork;

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
import com.codestates.mainproject.oneyearfourcut.global.aws.service.AwsS3Service;
import com.codestates.mainproject.oneyearfourcut.global.config.auth.jwt.JwtTokenizer;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GetArtworkTest {
    @Autowired
    private MockMvc mockMvc;
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
    private Gallery savedGallery;
    private Artwork savedArtwork;

    @BeforeEach
    void setup() {
        //회원 등록
        galleryMember = memberRepository.save(Member.builder()
                .nickname("test3")
                .email("test3@gmail.com")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .profile("/path1")
                .build());
        Member artworkMember = memberRepository.save(Member.builder()
                .nickname("test2")
                .email("test2@gmail.com")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .profile("/path2")
                .build());

        //전시회 등록
        savedGallery = galleryRepository.save(Gallery.builder()
                .member(galleryMember)
                .title("title")
                .content("content")
                .status(GalleryStatus.OPEN)
                .build());

        //작품 등록
        Artwork artwork = Artwork.builder()
                .title("artwork title")
                .content("artwork content")
                .build();
        artwork.setImagePath("/path/test1");
        artwork.setMember(artworkMember);
        artwork.setGallery(savedGallery);
        savedArtwork = artworkRepository.save(artwork);

        //전시회 주인이 작품에 좋아요 누름
        ArtworkLike like = new ArtworkLike();
        like.setArtwork(savedArtwork);
        like.setMember(galleryMember);
        like.setStatus(LikeStatus.LIKE);
        artworkLikeRepository.save(like);
    }

    @DisplayName("로그인 회원 조회: 좋아요 여부와 함께 조회")
    @Test
    void loginGetTest() throws Exception {
        //given
        //jwt 생성
        String jwt = jwtTokenizer.testJwtGenerator(galleryMember);

        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks/{artwork-id}",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(savedArtwork.getTitle()))
                .andExpect(jsonPath("$.content").value(savedArtwork.getContent()))
                .andExpect(jsonPath("$.liked").value(true));
    }

    @DisplayName("비로그인 조회: 좋아요 항상 false로 조회")
    @Test
    void anonymousGetTest() throws Exception {
        //given

        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks/{artwork-id}",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(savedArtwork.getTitle()))
                .andExpect(jsonPath("$.content").value(savedArtwork.getContent()))
                .andExpect(jsonPath("$.liked").value(false));
    }
}
