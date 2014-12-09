package com.sharethrough.sdk;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import com.sharethrough.android.sdk.R;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
    private ArgumentCaptor<Callback> placementCallbackArgumentCaptor;
    @Mock private BasicAdView mockAdView;
    @Mock private Placement placement;

    @Before
    public void setUp() throws Exception {
        adapter = mock(ListAdapter.class);
        when(adapter.getCount()).thenReturn(4);
        when(adapter.getItem(anyInt())).thenReturn(new String("feedItem"));

        when(placement.getArticlesBeforeFirstAd()).thenReturn(3);
        when(placement.getArticlesBetweenAds()).thenReturn(Integer.MAX_VALUE);

        when(mockAdView.getAdView()).thenReturn(mockAdView);

        sharethrough = mock(Sharethrough.class);

        when(sharethrough.getAdView(any(Context.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(IAdView.class))).thenReturn(mockAdView);
        sharethrough.placement = placement;
        subject = new SharethroughListAdapter(Robolectric.application, adapter, sharethrough, R.layout.ad, R.id.title, R.id.description, R.id.advertiser, R.id.thumbnail, R.id.optout, R.id.brand_logo);
        verify(adapter).registerDataSetObserver(any(DataSetObserver.class));

        placementCallbackArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(sharethrough).setOrCallPlacementCallback(placementCallbackArgumentCaptor.capture());
        placementCallbackArgumentCaptor.getValue().call(sharethrough.placement);
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
        assertThat(v).isSameAs(mockAdView);
    }

    @Test
    public void delegatesGetViewToWrappedAdapterForNonAd() throws Exception {
        View item = mock(View.class);
        when(adapter.getView(2, null, null)).thenReturn(item);

        View v = subject.getView(2, null, null);

        assertThat(v).isSameAs(item);
    }

    @Test
    public void getItem_forAd_returnsNull() {
        assertThat(subject.getItem(3)).isNull();
    }

    @Test
    public void getItem_forNonAd_returnsDelegatedGetItem() {
        final String[] strings = new String[100];
        for (int i = 0; i < strings.length; i++) {
            strings[i] =  "item_" + i;
        }

        when(adapter.getCount()).thenReturn(strings.length);
        when(adapter.getItem(anyInt())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                int position = (Integer) invocationOnMock.getArguments()[0];
                return strings[position];
            }
        });

        when(sharethrough.placement.getArticlesBeforeFirstAd()).thenReturn(2);
        when(sharethrough.placement.getArticlesBetweenAds()).thenReturn(1);
        placementCallbackArgumentCaptor.getValue().call(sharethrough.placement);
        assertThat(subject.getCount()).isEqualTo(199);
        assertThat(subject.getItem(0)).isSameAs(strings[0]);
        assertThat(subject.getItem(1)).isSameAs(strings[1]);
        assertThat(subject.getItem(2)).isNull();
        assertThat(subject.getItem(3)).isSameAs(strings[2]);
        assertThat(subject.getItem(4)).isNull();
        for (int i = 5; i < strings.length; i++) {
            assertThat(subject.getItem(i * 2 - 1)).isSameAs(strings[i]);
            assertThat(subject.getItem(i * 2)).isNull();
        }
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

    @Test
    public void whenPlacementBecomesAvailable_notifiesOfDatasetChange_onMainThread() throws Exception {
        final boolean[] wasChanged = new boolean[1];
        subject.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                wasChanged[0] = true;
            }
        });

        Robolectric.pauseMainLooper();

        when(sharethrough.placement.getArticlesBeforeFirstAd()).thenReturn(2);
        when(sharethrough.placement.getArticlesBetweenAds()).thenReturn(1);

        placementCallbackArgumentCaptor.getValue().call(sharethrough.placement);

        assertThat(wasChanged[0]).isFalse();
        Robolectric.unPauseMainLooper();
        assertThat(wasChanged[0]).isTrue();
    }

    @Test
    public void whenPlacementBecomesAvailable_placesAdsAppropriately() throws Exception {
        final View[] views = new View[100];
        for (int i = 0; i < views.length; i++) {
            views[i] = mock(View.class, "view_" + i);
        }

        when(adapter.getCount()).thenReturn(views.length);
        when(adapter.getView(anyInt(), any(View.class), any(ViewGroup.class))).thenAnswer(new Answer<View>() {
            @Override
            public View answer(InvocationOnMock invocationOnMock) throws Throwable {
                int position = (Integer) invocationOnMock.getArguments()[0];
                return views[position];
            }
        });

        when(sharethrough.placement.getArticlesBeforeFirstAd()).thenReturn(2);
        when(sharethrough.placement.getArticlesBetweenAds()).thenReturn(1);

        placementCallbackArgumentCaptor.getValue().call(sharethrough.placement);
        assertThat(subject.getCount()).isEqualTo(199);
        assertThat(subject.getView(0, null, mock(ViewGroup.class))).isSameAs(views[0]);
        assertThat(subject.getView(1, null, mock(ViewGroup.class))).isSameAs(views[1]);
        assertThat(subject.getView(2, null, mock(ViewGroup.class))).isInstanceOf(IAdView.class);
        assertThat(subject.getView(3, null, mock(ViewGroup.class))).isSameAs(views[2]);
        assertThat(subject.getView(4, null, mock(ViewGroup.class))).isInstanceOf(IAdView.class);
        for (int i = 5; i < views.length; i++) {
            assertThat(subject.getView(i * 2 - 1, null, mock(ViewGroup.class))).isSameAs(views[i]);
            assertThat(subject.getView(i * 2, null, mock(ViewGroup.class))).isInstanceOf(IAdView.class);
        }
    }

    @Test
    public void beforePlacementBecomesAvailable_showsNoAds_butWorksOK() throws Exception {
        final View[] views = {mock(View.class), mock(View.class), mock(View.class), mock(View.class), mock(View.class)};

        when(adapter.getCount()).thenReturn(views.length);
        when(adapter.getView(anyInt(), any(View.class), any(ViewGroup.class))).thenAnswer(new Answer<View>() {
            @Override
            public View answer(InvocationOnMock invocationOnMock) throws Throwable {
                int position = (Integer) invocationOnMock.getArguments()[0];
                return views[position];
            }
        });

        sharethrough.placement = mock(Placement.class);
        when(sharethrough.placement.getArticlesBetweenAds()).thenReturn(Integer.MAX_VALUE);
        when(sharethrough.placement.getArticlesBeforeFirstAd()).thenReturn(Integer.MAX_VALUE);
        subject = new SharethroughListAdapter(Robolectric.application, adapter, sharethrough, R.layout.ad, R.id.title, R.id.description, R.id.advertiser, R.id.thumbnail, R.id.optout, R.id.brand_logo);

        assertThat(subject.getCount()).isEqualTo(views.length);
        for (int i = 0; i < views.length; i++) {
            assertThat(subject.getView(i, null, mock(ViewGroup.class))).isSameAs(views[i]);
        }
    }
}