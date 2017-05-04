package com.genesys.knowledge.classifier.util;

import com.genesys.knowledge.classifier.exception.CategoryNotFoundException;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@Slf4j
public class CategoryHandler {

    /**
     * CategoriesOrderNumbers is a {@link Map} collection that contains {@link String} category id as a key
     * and {@link Integer} order number of a category in a map as a value.
     */
    private static Map<String, Integer> categoryOrderNumbers = new HashMap<>();

    /**
     * Initializes {@code this} CategoriesHandler with categories retrieved from the specified documents.
     */
    public void initHandler(Document[] documents) {
        for (Document document : documents) {
            initHandler(document);
        }
    }

    /**
     * Initializes {@code this} CategoriesHandler with categories retrieved from the specified document.
     */
    public void initHandler(Document document) {
        ArrayList<Category> categories = document.getCategories();
        for (Category category : categories) {
            this.addCategory(category);
        }
    }

    /**
     * Adds the specified category to {@code this} CategoriesHandler and returns {@code true}
     * in case there is no such a category in the map, otherwise returns {@code false} without adding.
     *
     * @param category {@code Category} instance to add to the map
     * @return {@code true} if there's no such a category in the map, otherwise - {@code false}
     */
    public boolean addCategory(Category category) {
        String categoryId = category.getId();
        if (categoryOrderNumbers.containsKey(categoryId)) {
            return false;
        } else {
            categoryOrderNumbers.put(categoryId, categoryOrderNumbers.size());
            return true;
        }
    }

    public int getCategoryOrderNumber(Category category) throws CategoryNotFoundException {
        if (categoryOrderNumbers.containsKey(category.getId())) {
            return categoryOrderNumbers.get(category.getId());
        } else {
            throw new CategoryNotFoundException(category); // TODO ask if architecturally right decision
        }
    }

    public int getCategoriesQuantity() {
        return categoryOrderNumbers.size();
    }

    /**
     * Gets id of the {@link Category} with the specified order number in {@code this} CategoriesHandler.
     *
     * @param categoryOrderNumber order number of the {@link Category} in {@code this} CategoriesHandler
     * @return {@code String} id of the category
     */
    public String getCategoryId(int categoryOrderNumber) {
        String categoryId = null;

        Set<Map.Entry<String, Integer>> entrySet = categoryOrderNumbers.entrySet();
        for (Map.Entry<String, Integer> entry : entrySet) {
            if (categoryOrderNumber == entry.getValue()) {
                categoryId = entry.getKey();
                break;
            }
        }

        return categoryId;
    }

    public static void serializeCategoryOrderNumbers() {
        String filePath = "src/main/resources/category/category-order-numbers.mahout";
        try (ObjectOutputStream outputStream =
                     new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)))) {
            outputStream.writeObject(categoryOrderNumbers);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void deserializeCategoryOrderNumbers() {
        String filePath = "src/main/resources/category/category-order-numbers.mahout";
        try (ObjectInputStream inputStream =
                     new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {
            categoryOrderNumbers = (HashMap) inputStream.readObject();
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Clears handler replacing all categories with a new empty container.
     */
    public void clear() {
        categoryOrderNumbers = Maps.newHashMap();
    }
}
