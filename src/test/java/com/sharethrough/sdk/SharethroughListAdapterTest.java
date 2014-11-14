package com.sharethrough.sdk;

import android.view.View;
import android.widget.ListAdapter;
import com.sharethrough.android.sdk.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class SharethroughListAdapterTest {

    private ListAdapter adapter;
    private SharethroughListAdapter subject;
    private Sharethrough sharethrough;

    @Before
    public void setUp() throws Exception {
        adapter = mock(ListAdapter.class);
        when(adapter.getCount()).thenReturn(4);
        when(adapter.getItem(anyInt())).thenReturn(new String("feedItem"));

        sharethrough = mock(Sharethrough.class);

        subject = new SharethroughListAdapter(Robolectric.application, adapter, sharethrough, R.layout.ad);
    }

    @Test
    public void getCount_returnsNumberOfItemsPlusTotalAds() throws Exception {
        assertThat(subject.getCount()).isEqualTo(adapter.getCount() + 1);
    }

    @Test
    public void getView_returnsTheProperViewFromTheOriginalAdapter() throws Exception {
        subject.getView(0, null, null);

        verify(adapter).getView(0, null, null);
//        verify(adapter).getItemViewType(0);

        subject.getView(3, null, null);
        verifyNoMoreInteractions(adapter);
        verify(sharethrough).putCreativeIntoAdView(any(IAdView.class), any(Runnable.class));
    }

    @Test
    public void inflatesProperLayout() throws Exception {
        View v = subject.getView(3, null, null);

        assertThat(v).isInstanceOf(IAdView.class);
    }

}