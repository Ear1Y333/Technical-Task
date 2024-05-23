package org.example;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final static String MESSAGE_FOR_NOT_RUSSIAN_PRODUCTS = "This product made not in Russia";
    private final static String MESSAGE_FOR_EXCEEDED_NUMBER_OF_REQUESTS = "Too many requests, please try again later";
    private final static String MESSAGE_FOR_SUCCESSFULLY_FILE_CREATE = "The file was successfully created";
    private final static String MESSAGE_FOR_UNSUCCESSFULLY_FILE_CREATE = "There are some problems, please try again later";
    private final int requestLimit;
    private final long intervalInNanos;
    private long lastRequestTime;
    private int requests;
    private final Lock lock;
    private final OkHttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        intervalInNanos = timeUnit.toNanos(1);
        this.requestLimit = requestLimit;
        this.lastRequestTime = System.nanoTime();
        this.requests = 0;
        this.lock = new ReentrantLock();
        this.httpClient = new OkHttpClient();
    }

    public Document fetchDocumentFromApi() throws IOException {
        String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }

            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String json = responseBody.string();
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, Document.class);
            } else {
                throw new IOException("Response body is null");
            }
        }
    }

    public String createDocumentForGoodsIntroduction(Document document, String name) {
        if (document.getProducerInn().equals("РФ")) {
            lock.lock();
            try {
                long currentTime = System.nanoTime();
                long elapsedTime = currentTime - lastRequestTime;

                if (elapsedTime < intervalInNanos && requests >= requestLimit) {
                    return MESSAGE_FOR_EXCEEDED_NUMBER_OF_REQUESTS;
                }
                lastRequestTime = currentTime;

                if (elapsedTime > intervalInNanos) {
                    requests = 0;
                }
                requests++;
            } finally {
                lock.unlock();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(name))) {
                writer.write(document.toString());
                return MESSAGE_FOR_SUCCESSFULLY_FILE_CREATE;
            } catch (IOException e) {
                return MESSAGE_FOR_UNSUCCESSFULLY_FILE_CREATE;
            }
        } else {
            return MESSAGE_FOR_NOT_RUSSIAN_PRODUCTS;
        }
    }

    static class Document {
        @JsonProperty("description")
        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        @JsonProperty("importRequest")
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("production_date")
        private Date productionDate;
        @JsonProperty("production_type")
        private String productionType;
        @JsonProperty("products")
        private List<Product> products;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("reg_date")
        private Date regDate;
        @JsonProperty("reg_number")
        private String regNumber;

        @Override
        public String toString() {
            return "Document{" +
                    "description=" + description +
                    ", docId='" + docId + '\'' +
                    ", docStatus='" + docStatus + '\'' +
                    ", docType='" + docType + '\'' +
                    ", importRequest=" + importRequest +
                    ", ownerInn='" + ownerInn + '\'' +
                    ", participantInn='" + participantInn + '\'' +
                    ", producerInn='" + producerInn + '\'' +
                    ", productionDate=" + productionDate +
                    ", productionType='" + productionType + '\'' +
                    ", products=" + products +
                    ", regDate=" + regDate +
                    ", regNumber='" + regNumber + '\'' +
                    '}';
        }

        public String getProducerInn() {
            return producerInn;
        }
    }

    static class Description {
        @JsonProperty("participantInn")
        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }
    }

    static class Product {
        @JsonProperty("certificate_document")
        private String certificateDocument;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("certificate_document_date")
        private Date certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonProperty("production_date")
        private Date productionDate;
        @JsonProperty("tnved_code")
        private String tnvedCode;
        @JsonProperty("uit_code")
        private String uitCode;
        @JsonProperty("uitu_code")
        private String uituCode;

        @Override
        public String toString() {
            return "Product{" +
                    "certificateDocument='" + certificateDocument + '\'' +
                    ", certificateDocumentDate=" + certificateDocumentDate +
                    ", certificateDocumentNumber='" + certificateDocumentNumber + '\'' +
                    ", ownerInn='" + ownerInn + '\'' +
                    ", producerInn='" + producerInn + '\'' +
                    ", productionDate=" + productionDate +
                    ", tnvedCode='" + tnvedCode + '\'' +
                    ", uitCode='" + uitCode + '\'' +
                    ", uituCode='" + uituCode + '\'' +
                    '}';
        }
    }
}