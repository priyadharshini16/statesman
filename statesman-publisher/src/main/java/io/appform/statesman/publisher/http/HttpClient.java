package io.appform.statesman.publisher.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author shashank.g
 */
@Slf4j
@AllArgsConstructor
public class HttpClient {

    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");
    private static final MediaType TEXT_PLAIN = MediaType.parse("text/plain");

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
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                postBuilder.addHeader(key, value);
            }
        }
        final Request request = postBuilder.build();
        return client.newCall(request).execute();
    }

    public Response postMultipartData(final String url,
                                      final Map<String, String> formFields,
                                      final List<String> files,
                                      final Map<String, String> headers) throws IOException {
        final HttpUrl httpUrl = HttpUrl.get(url);
        Request.Builder postBuilder;
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        if (formFields != null && !formFields.isEmpty()) {
            formFields.forEach(builder::addFormDataPart);
        }

        if (files != null && !files.isEmpty()) {
//            files.forEach((key, file) -> builder.addFormDataPart(attachment, file.getName(),
//                    RequestBody.create(MediaType.parse(getMediaType(file.getPath())), file)));
        }

        postBuilder = new Request.Builder()
                .url(httpUrl)
                .post(builder.build());

        if (headers != null) {
            headers.forEach(postBuilder::addHeader);
        }
        final Request request = postBuilder.build();
        return client.newCall(request).execute();
    }

    private String getMediaType(String path) {
//       return new MimetypesFileTypeMap().getContentType(path);
        return "image/png";
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
}
