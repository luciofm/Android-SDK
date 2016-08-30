package com.sharethrough.sdk;

public class Placement {
    private final int articlesBeforeFirstAd;
    private final int articlesBetweenAds;
    private final String promotedByText;
    private final String directSellPromotedByText;
    private final String status;

    public Placement(Response.Placement placement) {
        this.articlesBeforeFirstAd = placement.articlesBeforeFirstAd;
        this.articlesBetweenAds = placement.articlesBetweenAds;
        this.promotedByText = placement.promotedByText;
        this.directSellPromotedByText = placement.directSellPromotedByText;
        this.status = placement.status;
    }

    public int getArticlesBeforeFirstAd() {
        return articlesBeforeFirstAd;
    }

    public int getArticlesBetweenAds() {
        return articlesBetweenAds;
    }

    public String getPromotedByText() {
        return promotedByText;
    }

    public String getDirectSellPromotedByText() {
        return directSellPromotedByText;
    }

    public String getStatus() { return status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Placement placement = (Placement) o;

        if (articlesBeforeFirstAd != placement.articlesBeforeFirstAd) return false;
        if (articlesBetweenAds != placement.articlesBetweenAds) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = articlesBeforeFirstAd;
        result = 31 * result + articlesBetweenAds;
        return result;
    }

    @Override
    public String toString() {
        return "Placement{" +
                "articlesBeforeFirstAd=" + articlesBeforeFirstAd +
                ", articlesBetweenAds=" + articlesBetweenAds +
                '}';
    }
}
