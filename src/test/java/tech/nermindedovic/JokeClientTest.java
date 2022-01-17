package tech.nermindedovic;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JokeClientTest {

    @Test
    void randomizeMap() {
        Map<String, String> map = Map.of(
                "JA", "I am Joke for JA",
                "JB", "I am Joke for JB",
                "JC", "I am Joke for JC"
        );


        final Map<String, String> updatedMap = shuffleMap(map);

        assertNotEquals("I am Joke for JA",updatedMap.get("JA"));
        assertNotEquals("I am Joke for JB",updatedMap.get("JB"));
        assertNotEquals("I am Joke for JC",updatedMap.get("JC"));
    }


    @Test
    void testResponse() {
        getResponse();
    }


    private void getResponse() {


        final ServerResponseHandler serverResponse = new ServerResponseHandler("P:0");
        final ServerResponseHandler serverResponse1 = new ServerResponseHandler("P:1");
        final ServerResponseHandler serverResponse2 = new ServerResponseHandler("P:2");
        final ServerResponseHandler serverResponse3 = new ServerResponseHandler("P:3");
        final ServerResponseHandler serverResponse4 = new ServerResponseHandler("P:4");

        final ServerResponseHandler jokeResponse = new ServerResponseHandler("J:0");
        final ServerResponseHandler jokeResponse1 = new ServerResponseHandler("J:1");
        final ServerResponseHandler jokeResponse2 = new ServerResponseHandler("J:2");
        final ServerResponseHandler jokeResponse3 = new ServerResponseHandler("J:3");
        final ServerResponseHandler jokeResponse4 = new ServerResponseHandler("J:4");

        assertEquals("PA",serverResponse.convertToClientSideKey());
        assertEquals("PB",serverResponse1.convertToClientSideKey());
        assertEquals("PC",serverResponse2.convertToClientSideKey());
        assertEquals("PD",serverResponse3.convertToClientSideKey());
        assertEquals("PA",serverResponse4.convertToClientSideKey());


        assertEquals("JA",jokeResponse.convertToClientSideKey());
        assertEquals("JB",jokeResponse1.convertToClientSideKey());
        assertEquals("JC",jokeResponse2.convertToClientSideKey());
        assertEquals("JD",jokeResponse3.convertToClientSideKey());
        assertEquals("JA",jokeResponse4.convertToClientSideKey());

    }



    public static <K,V> Map<K,V> shuffleMap(Map<K,V> map) {
        final ArrayList<K> keyList = new ArrayList<>(map.keySet());
        List<V> valueList = new ArrayList<>(map.values());
        Collections.shuffle(keyList);
        Collections.shuffle(valueList);

        Map<K,V> shuffledMap = new HashMap<>();
        for (int i = 0; i < keyList.size(); i++) {
            shuffledMap.put(keyList.get(i), valueList.get(i));
        }
        return shuffledMap;
    }

}
