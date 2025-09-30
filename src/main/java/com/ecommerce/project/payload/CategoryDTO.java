package com.ecommerce.project.payload;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CategoryDTO {
    public Long categoryId;
    public String categoryName;
}
