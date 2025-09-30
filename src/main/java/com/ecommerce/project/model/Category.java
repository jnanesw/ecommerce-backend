package com.ecommerce.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  categoryId;

    @NotBlank
    @Size(min = 5, message = "Category name must contain atleast 5 character")
    private String categoryName;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    List<Product> products;
}
