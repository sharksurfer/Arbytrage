/**
 * Created by Trent Rand on 30/Sep/18.
 *
 * Ivory Tuna API - Binance
 * API Key: NzlusZKEFHbIPCRh5xFwunOBCEcas1CFug9xfDFuVMb21200pKf43zSjRJYAW8RU
 * API Secret: Grifip9joIzdyqv52noYVL5Lu0ZZYKjZjckXpVBDA8tCtj67YnJV6JrquvVay6Xl
 *
 * Binance ETH Address : 0x11fd895988f155eab4cfac9c9b40724e90b55ab5
 * Binance BTC Address : 1GyhgoddgV7fHeExVAoT8zaaDcb1HXmrAK
 * Binance LTC Address :
 * Binance XRP Address :
 *
 */

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BinanceUserDelegate extends TextWebSocketHandler {

    Portfolio self = Portfolio.getSelf();

    private String APIKey = "NzlusZKEFHbIPCRh5xFwunOBCEcas1CFug9xfDFuVMb21200pKf43zSjRJYAW8RU";
    private String APISecret = "Grifip9joIzdyqv52noYVL5Lu0ZZYKjZjckXpVBDA8tCtj67YnJV6JrquvVay6Xl";

    private String listenKey;


    public long serverTime() {
        String RequestURL  = "https://www.binance.com/api/v1/time";
        long toReturn = 0;

        try {
            URL url = new URL(RequestURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            con.setDoOutput(true);
            con.connect();

            int statusCode = con.getResponseCode();
            switch (statusCode) {

                //"Handle Successful Response"//
                case 200:
                    InputStreamReader inputReader = new InputStreamReader(con.getInputStream());
                    BufferedReader in = new BufferedReader(inputReader);

                    String tempLine;
                    String toParse = "";
                    while ((tempLine = in.readLine()) != null) {
                        toParse = tempLine;
                    }
                    try {
                        System.out.println("Line to parse: "+toParse);
                        JSONObject jsonInput = new JSONObject(toParse);

                        toReturn = jsonInput.getLong("serverTime");

                    } catch(Exception e) {
                        System.out.println("Parsing Error! ... "+e);
                    }

                    in.close();
                    return toReturn;

                default:

                    return toReturn;
            }


        } catch (Exception e) {
            System.out.print("Error setting up and executing HTTP Request. Prices are not real time!");
        }

        return toReturn;
    }

    public void fetchUserStream(int refresh) {
        String RequestURL  = "https://www.binance.com/api/v1/userDataStream";
            try {

                System.err.println("Attempting to fetch User Listen Key!");
                URL url = new URL(RequestURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                if(refresh == 1) {
                    con.setRequestMethod("PUT");
                } else {
                    con.setRequestMethod("POST");
                }

                con.setRequestProperty("X-MBX-APIKEY", "NzlusZKEFHbIPCRh5xFwunOBCEcas1CFug9xfDFuVMb21200pKf43zSjRJYAW8RU");

                con.setDoOutput(true);
                con.connect();

                int statusCode = con.getResponseCode();
                //System.err.println(statusCode);
                switch (statusCode) {

                    //"Handle Successful Response"//
                    case 200:
                        InputStreamReader inputReader = new InputStreamReader(con.getInputStream());
                        BufferedReader in = new BufferedReader(inputReader);

                        String tempLine;
                        String toParse = "";
                        while ((tempLine = in.readLine()) != null) {
                            toParse = tempLine;
                        }
                        //try {
                            //System.out.println("Line to parse: "+toParse);
                            JSONObject jsonInput = new JSONObject(toParse);

                            listenKey = jsonInput.getString("listenKey");

                            self.connectUserStream();

//                        } catch(Exception e) {
  //                          System.out.println("Parsing Error! ... "+e);
    //                    }

                        in.close();
                        return;

                    default:

                        return;
                }


            } catch (Exception e) {
                System.out.println("Error setting up and executing HTTP Request. Prices are not real time!");
            }

        return;
    }

    public String getListenKey() {
        if(listenKey == null) {
            fetchUserStream(0);
        }
        return listenKey;
    }

    public void setListenKey(String listenKey) {
        this.listenKey = listenKey;
    }

    public void updatePortfolio(String tradeSource) {
        //System.out.println(tradeSource);

        try {
            JSONObject tradeObject  = new JSONObject(tradeSource);
            //String pairString = tradeObject.getString("s");
            //CurrencyPair currPair = Portfolio.getSelf().binanceManager.fetchPairBySymbol(Portfolio.getSelf().binanceManager.activePairs, pairString.toLowerCase());

            String updateType = tradeObject.getString("e");
            if(updateType.equals("executionReport")) {

                System.out.println("Fetched execution Report!");

                if(tradeObject.getString("X").equals("FILLED")) {
                    //self.csvOutput.tradeLog = self.csvOutput.tradeLog+","+serverTime()+",Trade Filled!\n";
                    String symbol = tradeObject.getString("s");
                    String side = tradeObject.getString("S");
                    String strPrice = tradeObject.getString("p");

                    self.binanceManager.tradeFilled(symbol, side, strPrice);

                    //System.err.println(self.fetchDateInString()+" ... Holy shit! ... Trade Filled!");
                }
            }

        } catch (Exception e) {
            System.out.println("Error Handling Latest Data!");
            System.out.println("Error: " + e);
       }
    }

    public String encode(String key, String data) throws Exception {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
            return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign message.", e);
        }
    }

    //WebSocket Overrides.
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println("Message Received: " + message.getPayload());
        updatePortfolio(message.getPayload());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //String payload = "{\"type\": \"subscribe\",\"channels\":[{\"name\": \"ticker\",\"product_ids\": [\"BTC-USD\"]}]}";
        //session.sendMessage(new TextMessage(payload));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.out.println("Transport Error: "+ exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        System.out.println("Connection Closed [" + status.getReason() + "]");
    }
}