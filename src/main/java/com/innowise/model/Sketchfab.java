package com.innowise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sketchfab {
    private Long id;
    private String type;
    private String link;
    private String thumbnail;
}
