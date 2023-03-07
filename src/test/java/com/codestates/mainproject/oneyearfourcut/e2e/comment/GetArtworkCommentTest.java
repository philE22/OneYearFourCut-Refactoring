package com.codestates.mainproject.oneyearfourcut.e2e.comment;

import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.repository.ArtworkRepository;
import com.codestates.mainproject.oneyearfourcut.domain.comment.entity.Comment;
import com.codestates.mainproject.oneyearfourcut.domain.comment.repository.CommentRepository;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.repository.GalleryRepository;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.MemberStatus;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Role;
import com.codestates.mainproject.oneyearfourcut.domain.member.repository.MemberRepository;
import com.codestates.mainproject.oneyearfourcut.global.exception.exception.ExceptionCode;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GetArtworkCommentTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private GalleryRepository galleryRepository;
    @Autowired
    private ArtworkRepository artworkRepository;
    @Autowired
    private CommentRepository commentRepository;

    private Member galleryMember;
    private Member artworkMember;
    private Member commentMember;
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

        commentMember = memberRepository.save(Member.builder()
                .nickname("comment Writer")
                .email("comment@gmail.com")
                .profile("/path/comment")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    @DisplayName("작품 댓글까지 모두 조회된다.")
    @Test
    void successGetTest() throws Exception {
        //given
        Comment artworkComment1 = commentRepository.save(Comment.builder()
                .content("comment content")
                .member(commentMember)
                .gallery(savedGallery)
                .artwork(savedArtwork)
                .build());
        Comment artworkComment2 = commentRepository.save(Comment.builder()
                .content("comment content")
                .member(commentMember)
                .gallery(savedGallery)
                .artwork(savedArtwork)
                .build());
        Comment galleryComment1 = commentRepository.save(Comment.builder()
                .content("comment content")
                .member(commentMember)
                .gallery(savedGallery)
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .param("page", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.galleryId").value(savedGallery.getGalleryId()))
                .andExpect(jsonPath("$.pageInfo.totalElements").value(2))
                .andExpect(jsonPath("$.commentList[1].commentId").value(artworkComment1.getCommentId()))
                .andExpect(jsonPath("$.commentList[0].commentId").value(artworkComment2.getCommentId()));


    }
    @DisplayName("댓글이 없으면 빈 리스트가 조회된다.")
    @Test
    void noCommentTest() throws Exception {
        //given
        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .param("page", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.commentList.length()").value(0))
                .andExpect(jsonPath("$.pageInfo.totalElements").value(0))
                .andExpect(jsonPath("$.artworkId").value(savedArtwork.getArtworkId()));
    }
    @DisplayName("없는 전시회의 댓글을 조회하면 에러가 난다.")
    @Test
    void notExistGalleryTest() throws Exception {
        //given
        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        Integer.MAX_VALUE,
                        savedArtwork.getArtworkId())
                        .param("page", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.ARTWORK_NOT_FOUND_FROM_GALLERY.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.ARTWORK_NOT_FOUND_FROM_GALLERY.name()));
    }
}
