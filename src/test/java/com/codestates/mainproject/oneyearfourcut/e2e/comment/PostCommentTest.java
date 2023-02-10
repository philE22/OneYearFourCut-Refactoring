package com.codestates.mainproject.oneyearfourcut.e2e.comment;

import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.repository.ArtworkRepository;
import com.codestates.mainproject.oneyearfourcut.domain.comment.dto.CommentRequestDto;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.repository.GalleryRepository;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.MemberStatus;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Role;
import com.codestates.mainproject.oneyearfourcut.domain.member.repository.MemberRepository;
import com.codestates.mainproject.oneyearfourcut.global.config.auth.jwt.JwtTokenizer;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PostCommentTest {
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
    private JwtTokenizer jwtTokenizer;

    private Member galleryMember;
    private Member artworkMember;
    private Member commentMember;
    private Gallery savedGallery;
    private Artwork savedArtwork;
    private String jwt;

    @DisplayName("정상적인 작품 댓글 등록이 성공한다.")
    @Test
    void successArtworkCommentTest() throws Exception {
        //given
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

        commentMember = memberRepository.save(Member.builder()
                .nickname("comment Writer")
                .email("comment@gmail.com")
                .profile("/path/comment")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());

        jwt = jwtTokenizer.testJwtGenerator(commentMember);

        //when
        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .content("comment content")
                .build();
        String body = gson.toJson(commentRequestDto);

        ResultActions actions = mockMvc.perform(
                post("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.galleryId").value(savedGallery.getGalleryId()));
    }
























}

