package org.iota.ict.api;

import org.iota.ict.network.Neighbor;
import org.iota.ict.utils.Constants;
import org.iota.ict.utils.Stats;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JsonIctTest {

    @Test
    public void neighborStatsToScaledJSON() {
        Neighbor neighbor = generateNeighborWithRandomStats(Constants.API_MAX_STATS_PER_NEIGHBOR * 5);
        JSONArray array = JsonIct.neighborStatsToScaledJSON(neighbor);
        assertSumEquals(neighbor.getStatsHistory(), array);
    }

    private void assertSumEquals(List<Stats> statsHistory, JSONArray jsonArray) {
        Stats sumExpected = new Stats(statsHistory.get(0));
        for(int i = 1; i < statsHistory.size(); i++)
            sumExpected.accumulate(statsHistory.get(i));

        Stats sumActual = new Stats((Neighbor) null);
        for(int i = 0; i < jsonArray.length(); i++) {
            Stats fromJSON = new Stats(jsonArray.getJSONObject(i));
            sumActual.accumulate(fromJSON);
        }

        Assert.assertEquals("Incorrect sum of transactions.", sumExpected.receivedAll, sumActual.receivedAll);
    }

    private Neighbor generateNeighborWithRandomStats(int rounds) {
        Neighbor neighbor = new Neighbor("localhost:1234", 0);
        long timestamp = System.currentTimeMillis() - rounds * 100;
        neighbor.getStatsHistory().get(0).timestamp = timestamp;
        for(int i = 0; i < rounds; i++) {
            neighbor.getStats().receivedAll = (int)(Math.random() * 100);
            neighbor.getStats().timestamp = timestamp + i * 100;
            neighbor.newRound(0, false);
        }
        return neighbor;
    }
}