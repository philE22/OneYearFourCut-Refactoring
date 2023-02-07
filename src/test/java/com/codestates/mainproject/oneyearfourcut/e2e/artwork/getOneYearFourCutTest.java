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
import com.codestates.mainproject.oneyearfourcut.global.config.auth.jwt.JwtTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class getOneYearFourCutTest {
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
    private Member artworkMember;
    private Gallery savedGallery;

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
    }

    @DisplayName("좋아요가 높은 순으로 4개의 작품만 조회된다.")
    @Test
    void get4CutTest() throws Exception {
        /**
         * 총 작품 중에 좋아요가 높은 순으로 4개의 작품에 대한 정보만 반환해줌
         * 좋아요 수가 같으면 최신 등록 순으로 조회
         *
         * 좋아요 높은 작품 순서 : 0 > 1 > 4 = 3 = 2 > 5
         * 그러므로 0, 1, 4, 3 순으로 조회 된다
         */
        //given
        //작품 등록
        ArrayList<Artwork> artworkList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Artwork artwork = Artwork.builder()
                    .title("artwork title" + i)
                    .content("artwork content" + i)
                    .build();
            artwork.setImagePath("/path/test" + i);
            artwork.setMember(artworkMember);
            artwork.setGallery(savedGallery);

            artworkList.add(artwork);
        }
        List<Artwork> savedArtworkList = artworkRepository.saveAll(artworkList);

        //좋아요 누르는 회원 4명 등록
        ArrayList<Member> likeMemberList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            likeMemberList.add(memberRepository.save(Member.builder()
                    .nickname("like" + i)
                    .email("like" + i + "@gmail.com")
                    .role(Role.USER)
                    .status(MemberStatus.ACTIVE)
                    .profile("/path")
                    .build()));
        }

        //작품에 따른 좋아요 등록
        for (int i = 0; i < 4; i++) {
            ArtworkLike like = new ArtworkLike();
            like.setStatus(LikeStatus.LIKE);
            like.setArtwork(savedArtworkList.get(0));
            like.setMember(likeMemberList.get(i));

            artworkLikeRepository.save(like);
        }
        for (int i = 0; i < 3; i++) {
            ArtworkLike like = new ArtworkLike();
            like.setStatus(LikeStatus.LIKE);
            like.setArtwork(savedArtworkList.get(1));
            like.setMember(likeMemberList.get(i));

            artworkLikeRepository.save(like);
        }
        for (int i = 0; i < 2; i++) {
            ArtworkLike like = new ArtworkLike();
            like.setStatus(LikeStatus.LIKE);
            like.setArtwork(savedArtworkList.get(4));
            like.setMember(likeMemberList.get(i));

            artworkLikeRepository.save(like);
        }
        for (int i = 0; i < 2; i++) {
            ArtworkLike like = new ArtworkLike();
            like.setStatus(LikeStatus.LIKE);
            like.setArtwork(savedArtworkList.get(3));
            like.setMember(likeMemberList.get(i));

            artworkLikeRepository.save(like);
        }
        for (int i = 0; i < 2; i++) {
            ArtworkLike like = new ArtworkLike();
            like.setStatus(LikeStatus.LIKE);
            like.setArtwork(savedArtworkList.get(2));
            like.setMember(likeMemberList.get(i));

            artworkLikeRepository.save(like);
        }

        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks/like", savedGallery.getGalleryId())
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.[0].artworkId").value(savedArtworkList.get(0).getArtworkId()))
                .andExpect(jsonPath("$.[1].artworkId").value(savedArtworkList.get(1).getArtworkId()))
                .andExpect(jsonPath("$.[2].artworkId").value(savedArtworkList.get(4).getArtworkId()))
                .andExpect(jsonPath("$.[3].artworkId").value(savedArtworkList.get(3).getArtworkId()));
    }

    @DisplayName("작품이 없으면 빈 배열을 반환함")
    @Test
    void name() throws Exception {
        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks/like", savedGallery.getGalleryId())
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
