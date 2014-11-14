package com.sharethrough.sdk.network;

import com.sharethrough.sdk.Callback;
import com.sharethrough.sdk.Placement;
import com.sharethrough.sdk.TestBase;
import com.sharethrough.test.Fixtures;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.util.concurrent.ExecutorService;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PlacementTemplateFetcherTest extends TestBase {
    private static final String FIXTURE = Fixtures.getFile("assets/str_template.json");
    private static final String key = "abc";

    private PlacementTemplateFetcher subject;
    private ExecutorService executorService;
    @Mock private Callback<Placement> callback;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        executorService = mock(ExecutorService.class);
        subject = new PlacementTemplateFetcher(key, executorService);
    }

    @Test
    public void immediatelyAsksForTemplatePlacementInfo_inTheBackground() throws Exception {
        subject.fetch(callback);

        Robolectric.addHttpResponseRule("GET", "http://native.sharethrough.com/placements/" + key + "/template.json",
                new TestHttpResponse(200, FIXTURE));

        verifyNoMoreInteractions(callback);
        Misc.runLast(executorService);

        verify(callback).call(eq(new Placement(2, 3)));
    }
}