package com.genesys.knowledge.classifier.util;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@Slf4j
public class CategoriesHandler {

    /**
     * CategoriesOrderNumbers is a {@link Map} collection that contains {@link String} category id as a key
     * and {@link Integer} order number of a category in a map as a value.
     */
    private static Map<String, Integer> categoryOrderNumbers;

    public CategoriesHandler() {
        if (categoryOrderNumbers == null) {
            categoryOrderNumbers = new HashMap();
        }
    }

    /**
     * Initializes {@code this} {@link CategoriesHandler} populating {@link #categoryOrderNumbers}
     * with categories retrieved from the specified documents.
     */
    public void initHandler(Document[] documents) {
        for (Document document : documents) {
            Category[] categories = document.getCategories();
            for (Category category : categories) {
                this.addCategory(category);
            }
        }
    }

    public static void serializeCategoriesOrderNumbers() {
        String filePath = "src/main/resources/categoryOrderNumbers.mahout";
        try (ObjectOutputStream outputStream =
                     new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)))) {
            outputStream.writeObject(categoryOrderNumbers);
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace(); // TODO debug mode (remove lately)
        }
    }

    public static void deserializeCategoriesOrderNumbers() {
        String filePath = "src/main/resources/categoryOrderNumbers.mahout";
        try (ObjectInputStream inputStream =
                     new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {
            categoryOrderNumbers = (HashMap) inputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            e.printStackTrace(); // TODO debug mode (remove lately)
        }
    }

    /**
     * Adds the specified category to the {@link #categoryOrderNumbers} {@code Map<String, Integer>}
     * in case there is no such a category in the map, otherwise returns {@code false}.
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

    public int getCategoryOrderNumber(Category category) {
        return categoryOrderNumbers.get(category.getId());
    }

    public int getCategoriesNumber() {
        return categoryOrderNumbers.size();
    }
}
