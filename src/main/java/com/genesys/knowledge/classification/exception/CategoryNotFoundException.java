package com.genesys.knowledge.classification.exception;

import com.genesys.knowledge.domain.Category;

/**
 * Created by rhorilyi on 27.04.2017.
 */
public class CategoryNotFoundException extends Exception {

    public CategoryNotFoundException(Category category) {
        super("Category with id=" + category.getId() + " not found in CategoryHandler.");
    }
}
