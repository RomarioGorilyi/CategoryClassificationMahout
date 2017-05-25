package com.genesys.knowledge.classification.util;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import com.genesyslab.platform.commons.collections.KeyValueCollection;
import com.genesyslab.platform.commons.connection.configuration.ConnectionConfiguration;
import com.genesyslab.platform.commons.connection.configuration.KeyValueConfiguration;
import com.genesyslab.platform.commons.connection.tls.KeyManagerHelper;
import com.genesyslab.platform.commons.connection.tls.SSLContextHelper;
import com.genesyslab.platform.commons.connection.tls.TrustManagerHelper;
import com.genesyslab.platform.commons.protocol.ChannelState;
import com.genesyslab.platform.commons.protocol.Endpoint;
import com.genesyslab.platform.commons.protocol.Message;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import com.genesyslab.platform.openmedia.protocol.ExternalServiceProtocol;
import com.genesyslab.platform.openmedia.protocol.externalservice.request.Request3rdServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Created by jbert002 on 16/05/2017.
 */
public class SimpleEspClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("UT");
    private static final KeyValueCollection EMPTY_KV = new KeyValueCollection();

    public String name = "UnitTest";
    private String ucsHost = "gks-dep-stbl";
    private int ucsPort = 7102;
    private ExternalServiceProtocol connection;

    private long connectTimeout = 30000;
    private long requestTimeout = 30000;
    private Endpoint endpoint;

    public SimpleEspClient(String ucsHost, int ucsPort) {
        this.ucsHost = ucsHost;
        this.ucsPort = ucsPort;
    }

    public static Request3rdServer makeRequest3rdServer(String service, String method, KeyValueCollection parameters,
                                                        KeyValueCollection userData) {
        return makeRequest3rdServer(service, method, parameters, userData, EMPTY_KV);
    }

    public static Request3rdServer makeRequest3rdServer(String service, String method, KeyValueCollection parameters,
                                                        KeyValueCollection userData, KeyValueCollection requestorInfo) {
        KeyValueCollection keyValueCollection = asKeyValueCollection("AppName", "UnitTestClient", "AppType",
                "UnitTestAppType", "Service", service, "Method", method, "Parameters", parameters);

        if (requestorInfo != null) {
            keyValueCollection.addList("RequestorInfo", requestorInfo);
        }

        return Request3rdServer.create(keyValueCollection, userData);
    }

    public static KeyValueCollection asKeyValueCollection(Object... keyValues) {
        KeyValueCollection kvc = new KeyValueCollection();
        if (keyValues == null || keyValues.length == 0) {
            return kvc;
        }

        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("asKeyValueCollection requires an even number of parameters: " +
                    Arrays.toString(keyValues));
        }

        for (int i = 0; i < keyValues.length; ) {
            String key = (String) keyValues[i++];
            Object value = keyValues[i++];
            if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    addObject(kvc, key, item);
                }
            } else {
                addObject(kvc, key, value);
            }
        }
        return kvc;
    }

    private static void addObject(KeyValueCollection kvc, String key, Object value) {
        if (value instanceof String) {
            kvc.addString(key, (String) value);
        } else if (value instanceof Integer) {
            kvc.addInt(key, (Integer) value);
        } else if (value instanceof byte[]) {
            kvc.addBinary(key, (byte[]) value);
        } else if (value instanceof KeyValueCollection) {
            kvc.addList(key, (KeyValueCollection) value);
        }
    }

    /**
     * @return true if connection != null and connection.getState() = ChannelState.Opened
     */
    public synchronized boolean isConnected() {
        return (connection != null && (connection.getState() == ChannelState.Opened));
    }

    /**
     * Prepare a SSLContext for client to "trust" server always. SSLContext must be used when setting up endpoints.
     * <p/>
     * Starting from PSDK 8.1.100, we need SSLContext in case of auto-upgrade or full TLS. For UCS 8.1.3:
     * We should prepare SSLContext from configuration (PSDK TLSHelpers from apptemplate)
     * <br/> For UCS 8.1.2: keep backward compatibility: always trust server certificate!
     * Method is created here in CAL because we will require cfgobject reading
     * <p/>
     * See doc on GCafe PSDK
     */
    private SSLContext getClientSSLContext() {
        SSLContext sslContext = null;

        TrustManager trustManager = TrustManagerHelper.createTrustEveryoneTrustManager();
        KeyManager keyManager = KeyManagerHelper.createEmptyKeyManager();
        try {
            sslContext = SSLContextHelper.createSSLContext(keyManager, trustManager);
        } catch (GeneralSecurityException e) {
            LOGGER.warn("SSLContext cannot be generated. Secured connection may be unavailable : {}", e.getMessage());
        }

        return sslContext;
    }

    public synchronized void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Open a connection to UCS. Has not effect is already successfully connected.
     */
    public synchronized void open() throws InterruptedException, ProtocolException, IllegalStateException {
        if (!isConnected()) {

            if (endpoint == null) {
                KeyValueCollection kvCollection = new KeyValueCollection();
                kvCollection.addInt("upgrade", 1);
                ConnectionConfiguration config = new KeyValueConfiguration(kvCollection);

                endpoint = new Endpoint(name, ucsHost, ucsPort, config, false, getClientSSLContext(),
                        null);
            }
            connection = new ExternalServiceProtocol(endpoint);
            connection.open(connectTimeout);
        }

        if (connection.getState() != ChannelState.Opened) {
            fail("Could not connect to UCS ('" + ucsHost + ":" + ucsPort + "') within " + connectTimeout +
                    "ms, Status=" + connection.getState());
        }
    }

    /**
     * Close the UCS ESP connection. No effect if already successfully closed.
     */
    public synchronized void close() throws ProtocolException, IllegalStateException, InterruptedException {
        if (isConnected()) {
            connection.close(connectTimeout);
        }

        if (connection.getState() != ChannelState.Closed) {
            fail("Could not disconnect from UCS ('" + ucsHost + ":" + ucsPort + "')" + " within " + connectTimeout +
                    "ms, Status=" + connection.getState());
        }
    }

    private void fail(String s) {
        throw new RuntimeException(s);
    }

    public Message sendMessage(Message message) throws ProtocolException, IllegalStateException, InterruptedException {
        Message response = getConnection().request(message, requestTimeout);

        if (response == null) {
            fail("Request timed-out (" + requestTimeout + "ms)\n" + message);
        }

        return response;
    }

    // don't remove it
    public Message send(String service, String method, KeyValueCollection parameters, KeyValueCollection userData)
            throws ProtocolException, IllegalStateException, InterruptedException {
        return sendMessage(makeRequest3rdServer(service, method, parameters, userData));
    }

    public synchronized ExternalServiceProtocol getConnection() throws ProtocolException, InterruptedException {
        open();
        return connection;
    }


    public static void main(String[] args) {
        String url = "http://gks-dep-stbl:9092/gks-server/v1/kbs/langs/en/documents?tenantId=1&size=2000";
        String knowledgeBase = "bank_of_america";
        List<Document> documents = DocumentHandler.retrieveDocuments(url, knowledgeBase);

        Collections.shuffle(documents, new SecureRandom());
        List<Document> trainingDocuments = documents.subList(0, 4 * documents.size() / 5);
        List<Document> testDocuments = documents.subList(4 * documents.size() / 5, documents.size());

        SimpleEspClient client = new SimpleEspClient("gks-dep-stbl", 7102);
        try {
            client.open();
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

	private  void getTrainingDataObjects() throws InterruptedException, ProtocolException {
		Message trainingDataObjects = this.send("OMResponse", "GetTrainingDataObjects",
					asKeyValueCollection("TenantId", 1),
					null);
		System.out.println(trainingDataObjects);
	}

	private void getTrainingEmails(String trainingDataObjectId) throws InterruptedException, ProtocolException {
		Message trainingEmails = this.send("OMResponse", "GetTrainingEmails",
					asKeyValueCollection("TrainingDataObjectId", trainingDataObjectId),
					null);
			System.out.println(trainingEmails);
	}

	private void addCategoryRoot() throws InterruptedException, ProtocolException {
		this.send("OMResponse", "AddCategoryRoot",
					asKeyValueCollection(
							"Name", "testKnowledgeFaq",
							"TenantId", 1,
							"Language", "english",
							"Status", "Approved",
							"OwnerId", 100,
							"Type", 1),
					null);
	}

	private void addCategories(List<Document> documents) throws InterruptedException, ProtocolException {
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

	private void addTrainingEmails(String trainingDataObjectId, List<Document> documents) throws InterruptedException, ProtocolException {
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
