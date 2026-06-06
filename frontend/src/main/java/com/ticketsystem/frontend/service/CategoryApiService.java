package com.ticketsystem.frontend.service;

import com.ticketsystem.dto.request.CategoryRequest;
import com.ticketsystem.frontend.model.CategoryFX;
import com.ticketsystem.frontend.util.ApiClient;

import java.util.Arrays;
import java.util.List;

public class CategoryApiService {
    public List<CategoryFX> getAllCategories() throws Exception {
        CategoryFX[] arr = ApiClient.get("/categories", CategoryFX[].class);
        return Arrays.asList(arr);
    }

    public CategoryFX createCategory(String name, String desc) throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName(name);
        req.setDescription(desc);
        return ApiClient.post("/categories", req, CategoryFX.class);
    }
    
    public void deleteCategory(Long id) throws Exception {
        ApiClient.delete("/categories/" + id);
    }
}
