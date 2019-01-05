package org.iota.ict.api;

import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;

public class GithubGatewayTest {

    @Test
    public void testConnection() {
        JSONArray releases = GithubGateway.getReleases("iotaledger/ict");
        Assert.assertNotNull("Failed fetching releases from Github API.", releases);
        Assert.assertTrue("Expected releases but found none.", releases.length() > 0);
    }
}