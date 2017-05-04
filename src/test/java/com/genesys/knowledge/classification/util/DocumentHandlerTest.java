package com.genesys.knowledge.classifier.util;

import com.genesys.knowledge.domain.Document;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.genesys.knowledge.classifier.util.DocumentHandler.convertDocumentToTerms;
import static com.genesys.knowledge.classifier.util.DocumentHandler.findTermsNumberPerDocument;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by rhorilyi on 04.05.2017.
 */
public class DocumentHandlerTest {

    @Test
    public void testFindTermsNumberPerDocument() {
        System.out.println(findTermsNumberPerDocument().toString());
    }

    @Test
    public void testConvertDocumentToTerms() {
        Document document = new Document("My test text, which is a good enough text sample, is running...");
        List<String> actualTerms = convertDocumentToTerms(document);

        List<String> expectedTerms = Arrays.asList("test", "text", "good", "enough", "text", "sampl", "run");
        assertThat(actualTerms, is(expectedTerms));
    }

    @Test
    public void testConvertEmptyDocumentToTerms() {
        Document document = new Document();
        List<String> actualTerms = convertDocumentToTerms(document);

        List<String> expectedTerms = Collections.emptyList();
        assertThat(actualTerms, is(expectedTerms));
    }

    @Test(expected = NullPointerException.class)
    public void testConvertNullDocumentToTerms() {
        Document document = new Document(null);
        convertDocumentToTerms(document);
    }
}
