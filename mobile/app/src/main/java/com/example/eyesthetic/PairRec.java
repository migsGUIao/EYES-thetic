package com.example.eyesthetic;

public class PairRec {
    public String topName, topImageUrl;
    public String bottomName, bottomImageUrl;

    public PairRec() {} // for Firestore or JSON if needed

    public PairRec(String topName, String topImageUrl,
                   String bottomName, String bottomImageUrl) {
        this.topName = topName;
        this.topImageUrl = topImageUrl;
        this.bottomName = bottomName;
        this.bottomImageUrl = bottomImageUrl;
    }
}
