package com.sharethrough.sdk.network;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class IOExceptionHttpResponse extends BasicHttpResponse {
    public IOExceptionHttpResponse() {
        super(new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return null;
            }

            @Override
            public int getStatusCode() {
                return 500;
            }

            @Override
            public String getReasonPhrase() {
                return null;
            }
        });
    }

    @Override
    public HttpEntity getEntity() {
        return new HttpEntity() {
            @Override
            public boolean isRepeatable() {
                return false;
            }

            @Override
            public boolean isChunked() {
                return false;
            }

            @Override
            public long getContentLength() {
                return 0;
            }

            @Override
            public Header getContentType() {
                return null;
            }

            @Override
            public Header getContentEncoding() {
                return null;
            }

            @Override
            public InputStream getContent() throws IOException, IllegalStateException {
                throw new IOException("Stubbed IOException");
            }

            @Override
            public void writeTo(OutputStream outputStream) throws IOException {

            }

            @Override
            public boolean isStreaming() {
                return false;
            }

            @Override
            public void consumeContent() throws IOException {

            }
        };
    }
}
