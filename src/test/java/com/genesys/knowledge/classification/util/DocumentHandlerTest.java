package com.genesys.knowledge.classification.util;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.genesys.knowledge.classification.util.DocumentHandler.*;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by rhorilyi on 04.05.2017.
 */
public class DocumentHandlerTest {

    private String url = "http://gks-dep-stbl:9092/gks-server/v1/kbs/langs/en/documents?tenantId=1&size=2000";
    private final String knowledgeBase = "bank_of_america";

    @Test
    public void testRetrieveDocuments() {
        List<Document> documents = DocumentHandler.retrieveDocuments(url, knowledgeBase);
        assertThat(documents, is(not(empty())));
    }

    @Test
    public void testFindMaxNumberOfTokens() {
        Document doc1 = new Document("Test text");
        Document doc2 = new Document("Text");
        Document doc3 = new Document("Big long test text with an annex");
        List<Document> documents = Arrays.asList(doc1, doc2, doc3);

        assertThat(findMaxNumberOfTokens(documents), is(5));
    }

    @Test
    public void testFindUniqueCategoriesNumber() {
        Document mockDoc1 = mock(Document.class);
        when(mockDoc1.getCategories()).thenReturn(Arrays.asList(new Category("1"), new Category("2")));
        Document mockDoc2 = mock(Document.class);
        when(mockDoc2.getCategories()).thenReturn(Arrays.asList(new Category("2"), new Category("3")));
        Document mockDoc3 = mock(Document.class);
        when(mockDoc3.getCategories()).thenReturn(Collections.emptyList());
        List<Document> documents = Arrays.asList(mockDoc1, mockDoc2, mockDoc3);

        assertThat(findUniqueCategoriesNumber(documents), is(3));
    }

    @Test
    public void testConvertDocumentToTokensUsingFreeLingTokenizer() {
        String text = "My test text, which is a good enough text sample, is running out";
        List<String> actualTokens = convertTextToTokens(text, TokenizerType.FreeLingTokenizer);

        List<String> expectedTokens = Arrays.asList("test", "text", "good", "text", "sample", "run");
        assertThat(actualTokens, is(expectedTokens));
    }

    @Test
    public void testConvertDocumentToTokensUsingDefaultTokenizer() {
        String text = "My test text, which is a good enough text sample, is running out";
        List<String> actualTokens = convertTextToTokens(text, null);

        List<String> expectedTokens = Arrays.asList("test", "text", "good", "text", "sample", "run");
        assertThat(actualTokens, is(expectedTokens));
    }

    @Test
    public void testConvertDocumentToTokensUsingStandardTokenizer() {
        String text = "My test text, which is a good enough text sample, is running out";
        List<String> actualTokens = convertTextToTokens(text, TokenizerType.StandardTokenizer);

        List<String> expectedTokens = Arrays.asList("test", "text", "good", "enough", "text", "sampl", "run");
        assertThat(actualTokens, is(expectedTokens));
    }

    @Test
    public void testConvertEmptyDocumentToTokens() {
        List<String> actualTokens = convertTextToTokens("", null);

        List<String> expectedTokens = Collections.emptyList();
        assertThat(actualTokens, is(expectedTokens));
    }

    @Test(expected = NullPointerException.class)
    public void testConvertNullDocumentToTokens() {
        convertTextToTokens(null, null);
    }
}
