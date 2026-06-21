package com.ticketsystem.mapper;

import com.ticketsystem.dto.response.CategoryResponse;
import com.ticketsystem.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryResponse toResponse(Category category) {
        if (category == null) return null;
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setTemplateText(category.getTemplateText());
        return response;
    }
}
