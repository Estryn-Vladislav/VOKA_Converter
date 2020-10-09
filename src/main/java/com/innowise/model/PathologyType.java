package com.innowise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PathologyType {
    private Long id;
    private String name;
    private List<Sketchfab> sketchfabs;
}
