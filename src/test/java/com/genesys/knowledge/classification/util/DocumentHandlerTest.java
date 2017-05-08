package com.genesys.knowledge.classification.util;

import com.genesys.knowledge.domain.Document;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.genesys.knowledge.classification.util.DocumentHandler.*;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by rhorilyi on 04.05.2017.
 */
public class DocumentHandlerTest {

    private String urlTemplate = "http://gks-dep-stbl:9092/gks-server/v2/knowledge/tenants/1/langs/en_US/documents?size=%d&from=%d";

    @Test
    public void testRetrieveDocuments() {
        List<Document> documents = retrieveDocuments();
        assertThat(documents, is(not(empty())));
    }

    @Test
    public void testFindMaxNumberOfTerms() {
        System.out.println("Max number of terms: " + findMaxNumberOfTerms(retrieveDocuments()));
    }

    @Test
    public void testFindUniqueCategoriesNumber() {
        System.out.println("Unique categories number: " + findUniqueCategoriesNumber(retrieveDocuments()));
    }

    @Test
    public void testConvertDocumentToTerms() {
        String text = "My test text, which is a good enough text sample, is running out...";
        List<String> actualTerms = convertTextToTerms(text);

        List<String> expectedTerms = Arrays.asList("test", "text", "good", "enough", "text", "sampl", "run");
        assertThat(actualTerms, is(expectedTerms));
    }

    @Test
    public void testConvertEmptyDocumentToTerms() {
        List<String> actualTerms = convertTextToTerms("");

        List<String> expectedTerms = Collections.emptyList();
        assertThat(actualTerms, is(expectedTerms));
    }

    @Test(expected = NullPointerException.class)
    public void testConvertNullDocumentToTerms() {
        convertTextToTerms(null);
    }

    private List<Document> retrieveDocuments() {
        List<Document> documents = new ArrayList<>(1000);

        for (int i = 0; i < 3; i++) {
            String url = String.format(urlTemplate, 200, i * 200);
            documents.addAll(DocumentHandler.retrieveDocuments(url));
        }

        return documents;
    }
}
