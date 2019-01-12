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
        JSONArray releases = GithubGateway.getReleases(Constants.ICT_REPOSITORY);

        Assert.assertNotNull("Failed fetching releases from Github API.", releases);
        Assert.assertTrue("Expected releases but found none.", releases.length() > 0);
    }

    @Test
    public void testGetLatestReleaseLabel() {
        String label = GithubGateway.getLatestReleaseLabel(Constants.ICT_REPOSITORY);

        Assert.assertNotNull("Failed fetching label of latest release.", label);
        Assert.assertTrue("Unexpected label of most recent release: " + label, label.matches("0\\.[0-9]{1,2}(\\.[0-9])?"));
        Assert.assertTrue("Label of most recent release appears to be greater than current version.", VersionComparator.getInstance().compare(Constants.ICT_VERSION, label) > 0);
    }

    @Test
    public void testGetContents() {
        String contents = GithubGateway.getContents("iotaledger/ixi", "dev","versions.json");
        JSONObject versions = new JSONObject(contents);
        Assert.assertEquals("Unexpected content of iotaledger/ixi versions.json.","0.4", versions.getString("0.4"));
    }
}