package com.sharethrough.sdk;

import android.database.DataSetObserver;
import android.view.View;
import android.widget.AdapterView;
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

        subject = new SharethroughListAdapter(Robolectric.application, adapter, sharethrough, R.layout.ad, R.id.title, R.id.description, R.id.advertiser, R.id.thumbnail);
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

    @Test
    public void createsOnItemClickListener_thatReturnsDelegateOnNonAd() throws Exception {
        final int[] wasClick = new int[1];

        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                wasClick[0] = position;
            }
        };

        AdapterView.OnItemClickListener subjectListener = subject.createOnItemClickListener(listener);
        subjectListener.onItemClick(null, null, 8, 0);
        assertThat(wasClick[0]).isEqualTo(7);

        View adView = mock(View.class);
        subjectListener.onItemClick(null, adView, 3, 0);
        verify(adView).performClick();
        assertThat(wasClick[0]).isEqualTo(7);
    }

    @Test
    public void createsOnLongItemClickListener_thatReturnsDelegateOnNonAd() throws Exception {
        final int[] wasLongClick = new int[1];

        AdapterView.OnItemLongClickListener listener = new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                wasLongClick[0] = position;
                return false;
            }
        };

        AdapterView.OnItemLongClickListener subjectListener = subject.createOnItemLongClickListener(listener);
        subjectListener.onItemLongClick(null, null, 8, 0);

        assertThat(wasLongClick[0]).isEqualTo(7);

        View adView = mock(View.class);
        subjectListener.onItemLongClick(null, adView, 3, 0);
        verify(adView).performLongClick();
    }

    @Test
    public void createsOnItemSelectListener_thatReturnsDelegateOnNonAd() throws Exception {
        final int[] wasSelect = new int[1];
        final boolean[] wasNothing = new boolean[1];

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                wasSelect[0] = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                wasNothing[0] = true;
            }
        };

        AdapterView.OnItemSelectedListener subjectListener = subject.createOnItemSelectListener(listener);
        subjectListener.onItemSelected(null, null, 8, 0);
        assertThat(wasSelect[0]).isEqualTo(7);

        View adView = mock(View.class);
        subjectListener.onItemSelected(null, adView, 3, 0);
        assertThat(wasSelect[0]).isEqualTo(7);

        subjectListener.onNothingSelected(null);
        assertThat(wasNothing[0]).isTrue();
    }
}