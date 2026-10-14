package org.example.lab1.service;

import org.example.lab1.entity.Category;
import org.example.lab1.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("findAll: коли категорії існують — повертає список усіх категорій")
    void findAll_whenCategoriesExist_returnsCategoryList() {
        // Arrange
        Category cat1 = new Category(1L, "Streaming");
        Category cat2 = new Category(2L, "Games");
        when(categoryRepository.findAll()).thenReturn(List.of(cat1, cat2));

        // Act
        List<Category> result = categoryService.findAll();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Streaming", result.get(0).getName());
        assertEquals("Games", result.get(1).getName());
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("findAll: коли категорій немає — повертає порожній список")
    void findAll_whenNoCategoriesExist_returnsEmptyList() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(List.of());

        // Act
        List<Category> result = categoryService.findAll();

        // Assert
        assertTrue(result.isEmpty());
        verify(categoryRepository).findAll();
    }
}