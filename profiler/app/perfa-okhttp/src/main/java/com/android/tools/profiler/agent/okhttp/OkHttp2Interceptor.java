/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.profiler.agent.okhttp;

import com.android.tools.profiler.support.network.HttpConnectionTracker;
import com.android.tools.profiler.support.network.HttpTracker;
import com.android.tools.profiler.support.util.StudioLog;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okio.BufferedSource;
import okio.Okio;

public final class OkHttp2Interceptor implements Interceptor {

    public static void addToClient(Object client) {
        if (client instanceof OkHttpClient) {
            ((OkHttpClient) client).networkInterceptors().add(new OkHttp2Interceptor());
        }
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        HttpConnectionTracker tracker = null;
        try {
            tracker = trackRequest(request);
        } catch (Exception ex) {
            StudioLog.e("Could not track an OkHttp2 request", ex);
        }
        Response response = chain.proceed(request);
        try {
            response = track(tracker, response);
        } catch (Exception ex) {
            StudioLog.e("Could not track an OkHttp2 response", ex);
        }
        return response;
    }

    private HttpConnectionTracker trackRequest(Request request) {
        StackTraceElement[] callstack =
                OkHttpUtils.getCallstack(request.getClass().getPackage().getName());
        HttpConnectionTracker tracker = HttpTracker.trackConnection(request.urlString(), callstack);
        tracker.trackRequest(request.method(), toMultimap(request.headers()));
        return tracker;
    }

    private Response track(HttpConnectionTracker tracker, Response response) throws IOException {
        Map<String, List<String>> fields = toMultimap(response.headers());
        fields.put(
                "response-status-code",
                Collections.singletonList(Integer.toString(response.code())));
        tracker.trackResponse("", fields);

        BufferedSource source =
                Okio.buffer(
                        Okio.source(
                                tracker.trackResponseBody(response.body().source().inputStream())));
        ResponseBody body =
                ResponseBody.create(
                        response.body().contentType(), response.body().contentLength(), source);
        return response.newBuilder().body(body).build();
    }

    private Map<String, List<String>> toMultimap(Headers headers) {
        Map<String, List<String>> fields = new LinkedHashMap<String, List<String>>();
        for (String name : headers.names()) {
            fields.put(name, headers.values(name));
        }
        return fields;
    }
}