package com.ticketsystem.service;

import com.ticketsystem.dto.request.CategoryRequest;
import com.ticketsystem.dto.response.CategoryResponse;
import com.ticketsystem.exception.ResourceNotFoundException;
import com.ticketsystem.mapper.CategoryMapper;
import com.ticketsystem.model.Category;
import com.ticketsystem.repository.CategoryRepository;
import com.ticketsystem.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final TicketRepository ticketRepository;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper,
                            TicketRepository ticketRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.ticketRepository = ticketRepository;
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> {
                    CategoryResponse response = categoryMapper.toResponse(category);
                    // KAT-121: Anzahl Tickets pro Kategorie ergänzen
                    response.setTicketCount(ticketRepository.countByCategory(category));
                    return response;
                })
                .collect(Collectors.toList());
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Category already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        category.setTemplateText(request.getTemplateText());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found");
        }
        categoryRepository.deleteById(id);
    }
}
