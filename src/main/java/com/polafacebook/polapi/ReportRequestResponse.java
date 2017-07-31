package com.polafacebook.polapi;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Piotr on 12.07.2017.
 */
public class ReportRequestResponse {
    @SerializedName("signed_requests")
    private String[][] signedRequests;
    private int id;

    public ReportRequestResponse(String[][] signedRequests, int id) {
        this.id = id;
        this.signedRequests = signedRequests;
    }

    public ReportRequestResponse() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String[] getSignedRequests() {
        List<String> flatArray = new ArrayList<>();
        for (String[] array : signedRequests) {
            flatArray.addAll(Arrays.asList(array));
        }
        return flatArray.toArray(new String [flatArray.size()]);
    }

    public void setSignedRequests(String[][] signedRequests) {
        this.signedRequests = signedRequests;
    }

    @Override
    public String toString() {
        return "ReportRequestResponse{" +
                "id=" + id +
                ", signedRequests=" + Arrays.toString(this.getSignedRequests()) +
                '}';
    }
}
