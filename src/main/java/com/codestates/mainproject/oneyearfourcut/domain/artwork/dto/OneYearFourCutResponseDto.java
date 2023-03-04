package com.codestates.mainproject.oneyearfourcut.domain.artwork.dto;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
public class OneYearFourCutResponseDto {
    private long artworkId;
    private String imagePath;
    private int likeCount;

    @Builder
    private OneYearFourCutResponseDto(long artworkId, String imagePath, int likeCount) {
        this.artworkId = artworkId;
        this.imagePath = imagePath;
        this.likeCount = likeCount;
    }
}
