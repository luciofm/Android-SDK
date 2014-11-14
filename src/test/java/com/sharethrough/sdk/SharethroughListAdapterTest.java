package com.sharethrough.sdk;

import android.database.DataSetObserver;
import android.view.View;
import android.widget.ListAdapter;
import com.sharethrough.android.sdk.R;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

public class SharethroughListAdapterTest extends TestBase {

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
        verify(adapter).registerDataSetObserver(any(DataSetObserver.class));
    }

    @Test
    public void getCount_returnsNumberOfItemsPlusTotalAds() throws Exception {
        assertThat(subject.getCount()).isEqualTo(adapter.getCount() + 1);
    }

    @Test
    public void getView_returnsTheProperViewFromTheOriginalAdapter() throws Exception {
        subject.getView(0, null, null);

        verify(adapter).getView(0, null, null);

        subject.getView(3, null, null);
        verifyNoMoreInteractions(adapter);
    }

    @Test
    public void inflatesProperLayoutForAd() throws Exception {
        View v = subject.getView(3, null, null);

        assertThat(v).isInstanceOf(IAdView.class);

        verify(sharethrough).putCreativeIntoAdView(any(IAdView.class), any(Runnable.class));
    }

    @Test
    public void delegatesGetViewToWrappedAdapterForNonAd() throws Exception {
        View item = mock(View.class);
        when(adapter.getView(2, null, null)).thenReturn(item);

        View v = subject.getView(2, null, null);

        assertThat(v).isSameAs(item);
    }

    @Test
    public void getItemViewType_forAd_returnsDelegatedGetViewTypeCount() throws Exception {
        when(adapter.getViewTypeCount()).thenReturn(8);
        assertThat(subject.getItemViewType(3)).isEqualTo(8);
        when(adapter.getViewTypeCount()).thenReturn(80);
        assertThat(subject.getItemViewType(3)).isEqualTo(80);
    }

    @Test
    public void getItemViewType_forNonAd_returnsDelegatedGetItemViewType() throws Exception {
        when(adapter.getItemViewType(anyInt())).then(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (Integer) invocationOnMock.getArguments()[0];
            }
        });
        assertThat(subject.getItemViewType(0)).isEqualTo(0);
        assertThat(subject.getItemViewType(1)).isEqualTo(1);
        assertThat(subject.getItemViewType(2)).isEqualTo(2);
        assertThat(subject.getItemViewType(4)).isEqualTo(3);
    }

    @Test
    public void notifiesObserverWhenDelegatedAdapterChanges() throws Exception {
        final boolean[] wasChanged = new boolean[1];
        final boolean[] wasInvalidated = new boolean[1];
        subject.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                wasChanged[0] = true;
            }

            @Override
            public void onInvalidated() {
                wasInvalidated[0] = true;
            }
        });

        ArgumentCaptor<DataSetObserver> dataSetObserverArgumentCaptor = ArgumentCaptor.forClass(DataSetObserver.class);
        verify(adapter).registerDataSetObserver(dataSetObserverArgumentCaptor.capture());
        DataSetObserver dataSetObserver = dataSetObserverArgumentCaptor.getValue();

        dataSetObserver.onChanged();
        assertThat(wasChanged[0]).isTrue();

        dataSetObserver.onInvalidated();
        assertThat(wasInvalidated[0]).isTrue();
    }

    @Test
    public void hasStableIds_returnsDelegated() throws Exception {
        when(adapter.hasStableIds()).thenReturn(true);
        assertThat(subject.hasStableIds()).isTrue();
        when(adapter.hasStableIds()).thenReturn(false);
        assertThat(subject.hasStableIds()).isFalse();
    }

    @Test
    public void isEnabled_forNonAd_returnsDelegated() throws Exception {
        when(adapter.isEnabled(0)).thenReturn(true);
        when(adapter.isEnabled(3)).thenReturn(false);

        assertThat(subject.isEnabled(0)).isTrue();
        assertThat(subject.isEnabled(4)).isFalse();
    }

    @Test
    public void isEnabled_forAd_returnsTrue() throws Exception {
        assertThat(subject.isEnabled(3)).isTrue();
    }

    @Test
    public void areAllItemsEnabled_returnsDelegated() throws Exception {
        when(adapter.areAllItemsEnabled()).thenReturn(false);
        assertThat(subject.areAllItemsEnabled()).isFalse();
        when(adapter.areAllItemsEnabled()).thenReturn(true);
        assertThat(subject.areAllItemsEnabled()).isTrue();
    }
}