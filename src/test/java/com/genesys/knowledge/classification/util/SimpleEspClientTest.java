package com.genesys.knowledge.classification.util;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import com.genesyslab.platform.commons.protocol.Message;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import org.junit.Test;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Created by rhorilyi on 25.05.2017.
 */
public class SimpleEspClientTest {

    @Test
    public void testEspContentAnalyzer() {
        String url = "http://gks-dep-stbl:9092/gks-server/v1/kbs/langs/en/documents?tenantId=1&size=2000";
        String knowledgeBase = "bank_of_america";
        List<Document> allDocuments = DocumentHandler.retrieveDocuments(url, knowledgeBase);

        Collections.shuffle(allDocuments, new SecureRandom());
        List<Document> trainingDocuments = allDocuments.subList(0, 4 * allDocuments.size() / 5);
        List<Document> testDocuments = allDocuments.subList(4 * allDocuments.size() / 5, allDocuments.size());

        EspContentAnalyzer client = new EspContentAnalyzer("gks-dep-stbl", 7102);
        try {
            client.open();
//            client.addCategoryRoot("testKnowledgeFaq");
//            client.addCategories(allDocuments);
            client.addTrainingEmails("0000MaCHTT6Q093H", trainingDocuments);
            client.addTrainingEmails("0000MaCHTT6Q08QF", testDocuments);

        } catch (InterruptedException | ProtocolException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (ProtocolException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class EspContentAnalyzer extends SimpleEspClient {

        public EspContentAnalyzer(String ucsHost, int ucsPort) {
            super(ucsHost, ucsPort);
        }

        public void getTrainingDataObjects()
                throws InterruptedException, ProtocolException {
            Message trainingDataObjects = this.send("OMResponse", "GetTrainingDataObjects",
                    asKeyValueCollection("TenantId", 1),
                    null);
            System.out.println(trainingDataObjects);
        }

        public void getTrainingEmails(String trainingDataObjectId)
                throws InterruptedException, ProtocolException {
            Message trainingEmails = this.send("OMResponse", "GetTrainingEmails",
                    asKeyValueCollection("TrainingDataObjectId", trainingDataObjectId),
                    null);
            System.out.println(trainingEmails);
        }

        public void addCategoryRoot(String name)
                throws InterruptedException, ProtocolException {
            this.send("OMResponse", "AddCategoryRoot",
                    asKeyValueCollection(
                            "Name", name,
                            "TenantId", 1,
                            "Language", "english",
                            "Status", "Approved",
                            "OwnerId", 100,
                            "Type", 1),
                    null);
        }

        public void addCategories(List<Document> documents)
                throws InterruptedException, ProtocolException {
            for (Document document : documents) {
                for (Category category : document.getCategories()) {
                    this.send("OMResponse", "AddCategory",
                            asKeyValueCollection(
                                    "Id", category.getId(),
                                    "CategoryParentId", "0000MaCHTT6Q0199",
                                    "Name", category.getId(),
                                    "Status", "Approved",
                                    "OwnerId", 100,
                                    "Type", 1),
                            null);
                }
            }
        }

        public void addTrainingEmails(String trainingDataObjectId, List<Document> documents)
                throws InterruptedException, ProtocolException {
            for (Document document : documents) {
                for (Category category : document.getCategories()) {
                    this.send("OMResponse", "AddTrainingEmail",
                            asKeyValueCollection("Subject", "",
                                    "ReceivedDate", LocalDateTime.now().toString() + "Z",
                                    "Text", document.getText(),
                                    "CategoryId", category.getId(),
                                    "TrainingDataObjectId", trainingDataObjectId),
                            null);
                }
            }
        }
    }
}
