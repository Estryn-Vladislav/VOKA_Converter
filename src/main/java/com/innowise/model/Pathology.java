package com.innowise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pathology {
    private Long id;
    private String name;
    private List<PathologyType> types;
}
