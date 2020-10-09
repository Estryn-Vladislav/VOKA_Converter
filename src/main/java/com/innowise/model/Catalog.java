package com.innowise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catalog {
    private Long id;
    private String nameEn;
    private String nameDe;
    private String nameRu;
    private String state;
    private String icon;
}
