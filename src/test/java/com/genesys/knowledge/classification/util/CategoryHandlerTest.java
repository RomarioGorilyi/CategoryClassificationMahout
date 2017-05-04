package com.genesys.knowledge.classification.util;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by rhorilyi on 04.05.2017.
 */
public class CategoryHandlerTest {

    private CategoryHandler handler = new CategoryHandler();

    @Before
    public void clearHandler() {
        handler.clear();
    }

    @Test
    public void testInitHandler() {
        Document document = new Document();
        document.addCategory(new Category("category1"));
        document.addCategory(new Category("category2"));

        handler.initHandler(document);

        int actualCategoriesQuantity = handler.getCategoriesQuantity();
        int expectedQuantity = 2;
        assertEquals(expectedQuantity, actualCategoriesQuantity);
    }

    @Test
    public void testAddDuplicateCategories() {
        Category category = new Category("category");
        Category duplicateCategory = new Category("category");

        boolean categoryAdded = handler.addCategory(category);
        assertEquals(true, categoryAdded);

        boolean duplicateCategoryAdded = handler.addCategory(duplicateCategory);
        assertEquals(false, duplicateCategoryAdded);
    }

    @Test
    public void testClearHandler() {
        Document document = new Document();
        document.addCategory(new Category("category1"));
        document.addCategory(new Category("category2"));

        handler.initHandler(document);
        handler.clear();

        int actualCategoriesQuantity = handler.getCategoriesQuantity();
        int expectedQuantity = 0;
        assertEquals(expectedQuantity, actualCategoriesQuantity);
    }

    @Test
    public void testDeserializeCategoryOrderNumbers() {
        Category category = new Category("category");
        handler.addCategory(category);
        CategoryHandler.serializeCategoryOrderNumbers();

        handler.clear();

        int actualCategoriesQuantity = handler.getCategoriesQuantity();
        int expectedQuantity = 0;
        assertEquals(expectedQuantity, actualCategoriesQuantity);

        CategoryHandler.deserializeCategoryOrderNumbers();

        actualCategoriesQuantity = handler.getCategoriesQuantity();
        expectedQuantity = 1;
        assertEquals(expectedQuantity, actualCategoriesQuantity);
    }
}
