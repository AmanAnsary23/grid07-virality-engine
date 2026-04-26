package com.grid07.grid07.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "bots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String personaDescription;
}
