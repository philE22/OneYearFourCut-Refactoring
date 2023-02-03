package com.codestates.mainproject.oneyearfourcut.e2e.artwork;

import com.codestates.mainproject.oneyearfourcut.domain.Like.entity.ArtworkLike;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GetArtworksTest {
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
    @MockBean
    private AwsS3Service awsS3Service;

    Member galleryMember;
    Member artworkMember;
    Gallery gallery;
    String jwt;

    MockMultipartFile image = new MockMultipartFile(
            "image",
            "image.png",
            "image/png",
            "<<image.png>>".getBytes());

    @BeforeEach
    void beforeSetup() {
        //갤러리 주인 회원과 갤러리 생성
        galleryMember = memberRepository.save(Member.builder()
                .nickname("gallery Writer")
                .email("gallery@gmail.com")
                .profile("/path/gallery")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        gallery = galleryRepository.save(Gallery.builder()
                .title("gallery title")
                .content("gallery content")
                .member(galleryMember)
                .status(GalleryStatus.OPEN)
                .build());

        //작품 주인 회원과 토큰 생성
        artworkMember = memberRepository.save(Member.builder()
                .nickname("artwork Writer")
                .email("artwork@gmail.com")
                .profile("/path/artwork")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        jwt = jwtTokenizer.testJwtGenerator(artworkMember);

    }

    @DisplayName("로그인한 회원이 작품 조회하면, 좋아요 여부와 함께 정상 조회된다.")
    @Test
    void findArtworkListTest() throws Exception {
        //given
        for (int i = 1; i < 5; i++) {
            Artwork artwork = Artwork.builder()
                    .title("artwork" + i)
                    .content("content" + i)
                    .image(image)
                    .build();
            artwork.setMember(artworkMember);
            artwork.setGallery(gallery);
            artwork.setImagePath("/path" + i);
            Artwork savedArtwork = artworkRepository.save(artwork);
            if (i < 2) {
                ArtworkLike like = new ArtworkLike();
                like.setArtwork(savedArtwork);
                like.setMember(artworkMember);
                artworkLikeRepository.save(like);
            }
        }


        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks", gallery.getGalleryId())
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.[0].title").value("artwork4"))
                .andExpect(jsonPath("$.[3].title").value("artwork1"))
                .andExpect(jsonPath("$.[0].liked").value(false))
                .andExpect(jsonPath("$.[3].liked").value(true));
    }

    @DisplayName("비 로그인 상태로 작품조회하면, 좋아요 상태 없이 함께 정상 조회된다.")
    @Test
    void anonymousFindTest() throws Exception {
        //given
        for (int i = 1; i < 5; i++) {
            Artwork artwork = Artwork.builder()
                    .title("artwork" + i)
                    .content("content" + i)
                    .image(image)
                    .build();
            artwork.setMember(artworkMember);
            artwork.setGallery(gallery);
            artwork.setImagePath("/path" + i);
            Artwork savedArtwork = artworkRepository.save(artwork);

            ArtworkLike like = new ArtworkLike();
            like.setArtwork(savedArtwork);
            like.setMember(artworkMember);
            artworkLikeRepository.save(like);
        }


        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks", gallery.getGalleryId())
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.[0].title").value("artwork4"))
                .andExpect(jsonPath("$.[3].title").value("artwork1"))
                .andExpect(jsonPath("$.[0].liked").value(false));
    }

    @DisplayName("작품이 없는 경우 빈 배열이 조회된다.")
    @Test
    void emptyArtworkListTest() throws Exception {

        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks", gallery.getGalleryId())
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
