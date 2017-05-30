package com.genesys.knowledge;

import com.genesys.knowledge.classification.classifier.ClassifierConstructedWithDocumentsTest;
import com.genesys.knowledge.classification.classifier.LrClassifierConstructedWithFixedCategoriesAndFeaturesNumbersTest;
import com.genesys.knowledge.classification.util.CategoryHandlerTest;
import com.genesys.knowledge.classification.util.DocumentHandlerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by rhorilyi on 08.05.2017.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        CategoryHandlerTest.class,
        DocumentHandlerTest.class,
        LrClassifierConstructedWithFixedCategoriesAndFeaturesNumbersTest.class,
        ClassifierConstructedWithDocumentsTest.class
})
public class JunitTestSuite {
}
