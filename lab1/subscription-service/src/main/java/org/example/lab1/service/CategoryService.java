package org.example.lab1.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.lab1.entity.Category;
import org.example.lab1.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
}
