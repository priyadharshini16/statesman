package io.appform.statesman.publisher.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.statesman.model.action.template.httpaction.FormData;
import io.appform.statesman.model.action.template.httpaction.FormFieldType;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * @author shashank.g
 */
@Slf4j
@AllArgsConstructor
public class HttpClient {

    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");

    public final ObjectMapper mapper;
    public final OkHttpClient client;

    public Response post(String url,
                         final Object payload,
                         final Map<String, String> headers) throws IOException {
        final HttpUrl httpUrl = HttpUrl.get(url);
        Request.Builder postBuilder;
        if(payload instanceof String) {
             postBuilder =  new Request.Builder()
                     .url(httpUrl)
                     .post(RequestBody.create(APPLICATION_JSON, (String)payload));
        }
        else {
            postBuilder = new Request.Builder()
                    .url(httpUrl)
                    .post(RequestBody.create(APPLICATION_JSON, mapper.writeValueAsBytes(payload)));
        }
        if (headers != null) {
            headers.forEach(postBuilder::addHeader);
        }
        final Request request = postBuilder.build();
        return client.newCall(request).execute();
    }

    public Response postMultipartData(final String url,
                                      final List<FormData> formData,
                                      final Map<String, String> headers) throws IOException {
        final HttpUrl httpUrl = HttpUrl.get(url);
        MultipartBody.Builder meb = new MultipartBody.Builder().setType(MultipartBody.FORM);

        formData.forEach(data -> {
            if(data.getType() == FormFieldType.TEXT) {
                meb.addFormDataPart(data.getKey(), data.getValue());
            } else {
                meb.addFormDataPart("attachments[]", data.getKey(), getFileRequest(data.getValue()));
            }
        });

        Request.Builder postBuilder;
        postBuilder = new Request.Builder()
                .url(httpUrl)
                .post(meb.build());

        if (headers != null) {
            headers.forEach(postBuilder::addHeader);
        }
        final Request request = postBuilder.build();
        return client.newCall(request).execute();
    }

    public Response get(final String url,
                        final Map<String, String> headers) throws IOException {
        final HttpUrl httpUrl = HttpUrl.get(url);
        final Request.Builder getBuilder = new Request.Builder()
                .url(httpUrl)
                .get();
        if (headers != null) {
            headers.forEach(getBuilder::addHeader);
        }
        final Request request = getBuilder.build();
        return client.newCall(request).execute();
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
