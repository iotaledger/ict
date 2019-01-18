package org.iota.ict.api;

import org.iota.ict.utils.Constants;
import org.iota.ict.utils.VersionComparator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class GithubGatewayTest {

    @Test
    public void testGetReleases() {
        if(Math.random() < 0.9) // run randomly to safe time and to avoid api rate limits
            return;
        JSONArray releases = GithubGateway.getReleases(Constants.ICT_REPOSITORY);
        Assert.assertNotNull("Failed fetching releases from Github API.", releases);
        Assert.assertTrue("Expected releases but found none.", releases.length() > 0);
    }

    @Test
    public void testGetLatestReleaseLabel() {
        if(Math.random() < 0.9) // run randomly to safe time and to avoid api rate limits
            return;
        String label = GithubGateway.getLatestReleaseLabel(Constants.ICT_REPOSITORY);
        Assert.assertNotNull("Failed fetching label of latest release.", label);
        Assert.assertTrue("Unexpected label of most recent release: " + label, label.matches("0\\.[0-9]{1,2}(\\.[0-9])?"));
        Assert.assertTrue("Label of most recent release ('"+label+"') appears to be greater than or equal to current version ("+Constants.ICT_VERSION+").", VersionComparator.getInstance().compare(Constants.ICT_VERSION, label) > 0);
    }

    @Test
    public void testGetContents() {
        if(Math.random() < 0.9) // run randomly to safe time and to avoid api rate limits
            return;
        String contents = GithubGateway.getContents("iotaledger/ixi", "dev","versions.json");
        JSONObject versions = new JSONObject(contents);
        Assert.assertEquals("Unexpected content of iotaledger/ixi versions.json.","0.4", versions.getString("0.4"));
    }
}