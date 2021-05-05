package io.appform.statesman.engine.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import io.appform.statesman.model.action.freshdeskticket.CreateFDTicketWithAttachmentRequest;
import io.appform.statesman.model.action.freshdeskticket.CreateSimpleFDTicketRequest;
import io.appform.statesman.model.action.freshdeskticket.FieldType;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.http.HttpUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.minidev.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.file.Files;
import java.util.Map;

@Slf4j
@Singleton
public class FreshDeskCommands {

    private Provider<HttpClient> client;
    private static String url = "https://telemeds.freshdesk.com/api/v2/tickets";

    @Inject
    FreshDeskCommands(@Named("freshDeskHttpClient") Provider<HttpClient> client){
        this.client = client;
    }

    @SneakyThrows
    public String createTicket(final CreateSimpleFDTicketRequest request) {
        Map<String, String> headers = Maps.newHashMap();
        headers.put("authorization", request.getAuthorization());
        Response response = null;
        try {
            response = client.get().post(url, request.getPayload(), headers);
        } catch (IOException e) {
            log.error("Error occurred while creating ticket on fresh desk, request: {}", request);
            throw new StatesmanError();
        }

        if (!response.isSuccessful()) {
            log.error("Error occurred while creating ticket on fresh desk, request: {}, response: {}", request,
                    HttpUtil.body(response));
            throw new StatesmanError();
        }

        val responseBodyStr = HttpUtil.body(response);
        if (Strings.isNullOrEmpty(responseBodyStr)) {
            log.error("Response Body  is null for create fresh desk ticket request {}", request);
            throw new StatesmanError();
        }

        log.debug("HTTP Response: {}", responseBodyStr);

        JsonNode jsonNode = new ObjectMapper().readTree(responseBodyStr);
        return jsonNode.get("id").asText();
    }

    private static ResponseBody responseBody(Response response) throws InvalidObjectException {
        if (null == response || response.body() == null) {
            throw new InvalidObjectException("response or response body is null");
        }
        return response.body();
    }

    @SneakyThrows
    public String createTicketWithAttachment(CreateFDTicketWithAttachmentRequest request) {
        Map<String, String> headers = Maps.newHashMap();
        headers.put("authorization", request.getAuthorization());
        headers.put("content-type", "multipart/form-data");
        Response response = null;

        MultipartBody.Builder meb = new MultipartBody.Builder().setType(MultipartBody.FORM);

        request.getFdTicketFormData().forEach(data -> {
            if(data.getType() == FieldType.TEXT) {
                meb.addFormDataPart(data.getKey(), data.getValue());
            } else {
                meb.addFormDataPart("attachments[]", data.getKey(), getFileRequest(data.getValue()));
            }
        });

        try {
            response = client.get().postMultipartData(url, meb.build(), headers);
        } catch (IOException e) {
            log.error("Error occurred while creating ticket on fresh desk, request: {}", request);
            throw new StatesmanError();
        }

        if (!response.isSuccessful()) {
            log.error("Error occurred while creating ticket on fresh desk, request: {}, response: {}", request,
                    HttpUtil.body(response));
            throw new StatesmanError();
        }

        val responseBodyStr = HttpUtil.body(response);
        if (Strings.isNullOrEmpty(responseBodyStr)) {
            log.error("Response Body  is null for create fresh desk ticket request {}", request);
            throw new StatesmanError();
        }

        log.debug("HTTP Response: {}", responseBodyStr);

        JsonNode jsonNode = new ObjectMapper().readTree(responseBodyStr);
        return jsonNode.get("id").asText();
    }

    private RequestBody getFileRequest(String value) {
        File file = new File(value);
        try {
            return RequestBody.create(MediaType.parse("image/png"),
                    Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new StatesmanError();
        }
    }
}
