package com.codestates.mainproject.oneyearfourcut.e2e.artwork;

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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DeleteArtworkTest {
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

    private Gallery savedGallery;
    private Artwork savedArtwork;
    private Member artworkMember;
    private Member galleryMember;
    @BeforeEach
    void setup() {
        //회원 등록
        artworkMember = memberRepository.save(Member.builder()
                .nickname("test2")
                .email("test2@gmail.com")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .profile("/path2")
                .build());
        galleryMember = memberRepository.save(Member.builder()
                .nickname("test3")
                .email("test3@gmail.com")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .profile("/path1")
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
                .title("title")
                .content("content")
                .build();
        artwork.setGallery(savedGallery);
        artwork.setMember(artworkMember);
        artwork.setImagePath("/path/test");

        savedArtwork = artworkRepository.save(artwork);
    }
    @AfterEach
    void clear() {
        artworkRepository.deleteAll();
        galleryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @DisplayName("정상적인 삭제요청은 성공한다.")
    @Test
    void successDeleteTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(artworkMember);

        //when
        ResultActions actions = mockMvc.perform(
                delete("/galleries/{gallery-id}/artworks/{artwork-id}",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .header("Authorization", jwt)
        );
        List<Artwork> list = artworkRepository.findAll();

        //then
        actions.andExpect(status().isNoContent());
        Assertions.assertThat(list.size()).isEqualTo(0);
    }

    @DisplayName("작품 작성자가 아니면 실패한다.")
    @Test
    void authTest() throws Exception {
        //given
        Member member = memberRepository.save(Member.builder()
                .nickname("test4")
                .email("test4@gmail.com")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .profile("/path1")
                .build());
        String jwt = jwtTokenizer.testJwtGenerator(member);

        //when
        ResultActions actions = mockMvc.perform(
                delete("/galleries/{gallery-id}/artworks/{artwork-id}",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .header("Authorization", jwt)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.UNAUTHORIZED.getStatus()));
        actions.andExpect(jsonPath("$.exception").value(ExceptionCode.UNAUTHORIZED.name()));
    }
}
