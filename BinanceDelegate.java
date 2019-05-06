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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Hex;

import org.springframework.core.annotation.Order;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import org.json.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class BinanceDelegate extends TextWebSocketHandler {

    Portfolio self = Portfolio.getSelf();

    private String APIKey = "NzlusZKEFHbIPCRh5xFwunOBCEcas1CFug9xfDFuVMb21200pKf43zSjRJYAW8RU";
    private String APISecret = "Grifip9joIzdyqv52noYVL5Lu0ZZYKjZjckXpVBDA8tCtj67YnJV6JrquvVay6Xl";



    public Orderbook getOrderBook(CurrencyPair pair) {
        Orderbook toReturn = new Orderbook();
        toReturn.pair = pair;

        String urlString = "https://www.binance.com/api/v1/depth?symbol="+pair.getPairName().toUpperCase()+"&limit=1000";

        try {
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoOutput(true);

            //Setup HTTP Headers//
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);

            con.connect();

            //Parse HTTP Response Code//
            int statusCode = con.getResponseCode();
            switch (statusCode) {

                //"Handle Successful Response"//
                case 200:

                    //System.out.println("Status 200! We're good to Parse! Output incoming!");

                    //Successful Connection, attempting to parse response!//
                    InputStreamReader inputReader = new InputStreamReader(con.getInputStream());
                    BufferedReader in = new BufferedReader(inputReader);

                    String tempLine;
                    String toParse = "";
                    StringBuffer content = new StringBuffer();
                    while ((tempLine = in.readLine()) != null) {
                        toParse = tempLine;
                    }
                    try {
                        JSONObject jsonInput = new JSONObject(toParse);
                        //System.out.println("Line to parse: "+jsonInput);

                        JSONArray asksArray = jsonInput.getJSONArray("asks");
                        for(int i = 0; i < asksArray.length(); i++) {
                            JSONArray askItem = asksArray.getJSONArray(i);

                            double askPrice = askItem.getDouble(0);
                            double askVol = askItem.getDouble(1);

                            LiveOrder ask = new LiveOrder(askPrice, 0, askVol);
                            toReturn.asks.add(ask);

                            //pair.setAsk(bestAsk);
                        }

                        JSONArray bidsArray = jsonInput.getJSONArray("bids");
                        for(int t = 0; t < bidsArray.length(); t++) {
                            JSONArray bidItem = bidsArray.getJSONArray(t);

                            double bidPrice = bidItem.getDouble(0);
                            double bidVol = bidItem.getDouble(1);

                            LiveOrder bid = new LiveOrder(bidPrice, 1, bidVol);
                            toReturn.bids.add(bid);

                            //pair.setBid(bestBid);
                        }
                        //System.out.println("Pair Fetched!");
                        //System.out.println("Pair: "+pair.getPairName()+" Bid: "+toReturn.bids.get(0).getPrice()+" Ask: "+toReturn.asks.get(0).getPrice());

                    } catch(Exception e) {
                        System.out.println("Parsing Error! ... "+e);
                        //temp.source = "Error parsing input!";
                    }

                    in.close();

                    //System.out.println("Returning Complete SetupArray!");
                    toReturn.sortBooks();

                    pair.setDepth(toReturn);

                    return toReturn;

                //"Strike Colors!"//
                default:
                    System.out.println("HTTP Error: ... ");
                    return toReturn;
            }


        } catch (Exception e) {
            System.out.print("Error setting up and executing HTTP Request. Prices are not real time!");
        }
        //toReturn.sortBooks();

        return toReturn;
    }


    public void ExecuteTrade(CurrencyPair pair, String side, double limitPrice, String vol) {
        Portfolio self = Portfolio.getSelf();

        //Example POST: https://api.binance.com/api/v3/order?symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559&signature=c8db56825ae71d6d79447849e617115f4a920fa2acdcab2b053c4b2838bd6b71
        //String hardparams = "symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.05&recvWindow=5000&timestamp=1499827319559";

        String baseURL = "https://api.binance.com/api/v3/order?"; // order/test?
        String baseParams = "symbol="+pair.getPairName().toUpperCase()+"&side="+side+"&type=LIMIT&timeInForce=GTC&quantity="+String.valueOf(vol)+"&price="+limitPrice; //"
        String recWindow = "3000";
        String timestamp = ""+serverTime();
        String params = baseParams+"&recvWindow="+recWindow+"&timestamp="+timestamp;
        String signature = "null"; try { signature = encode(APISecret, params); } catch (Exception e) { System.err.println("Error signing Order Request! ... "+e); }
        String signedURLString = baseURL+params+"&signature="+signature;


        //String hardRequest = "symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559&signature="+signature;

        System.out.println(signedURLString);

        try {
            URL url = new URL(signedURLString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            con.setRequestProperty("X-MBX-APIKEY", "NzlusZKEFHbIPCRh5xFwunOBCEcas1CFug9xfDFuVMb21200pKf43zSjRJYAW8RU");

            con.setDoOutput(true);
            //con.setDoInput(true);

            //OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            //wr.write();
            //wr.flush();

            con.connect();

            //Parse HTTP Response Code//
            int statusCode = con.getResponseCode();
            System.out.println(con.getResponseMessage());
            //System.out.println(con.getErrorStream());

            switch (statusCode) {

                //"Handle Successful Response"//
                case 200:
                    //System.out.println("Status 200! We're good to Parse! Output incoming!");
                    //Successful Connection, attempting to parse response!//
                    InputStreamReader inputReader = new InputStreamReader(con.getInputStream());
                    BufferedReader in = new BufferedReader(inputReader);

                    String tempLine;
                    String toParse = "";
                    while ((tempLine = in.readLine()) != null) {
                        toParse = tempLine;
                    }
                    try {
                        JSONObject jsonInput = new JSONObject(toParse);
                        System.out.println("Line to parse: "+toParse);

                        double price = Double.parseDouble(jsonInput.getString("price"));
                        double volume = Double.parseDouble(jsonInput.getString("origQty"));
                        String orderSide = jsonInput.getString("side");
                        int orderID = jsonInput.getInt("orderId");

                        LiveOrder saveOrder = new LiveOrder(price, orderSide, volume, orderID);

                        if(side.equals("SELL")) {
                            pair.activeAsk = saveOrder;
                        } else if (side.equals("BUY")) {
                            pair.activeBid = saveOrder;
                        }



                    } catch(Exception e) {
                        System.out.println("Parsing Error! ... "+e);
                    }

                    in.close();
                    return ;

                default:
                    System.err.println("HTTP Response Error ... "+statusCode);
                    try {
                    InputStreamReader iReader = new InputStreamReader(con.getErrorStream());
                    BufferedReader i = new BufferedReader(iReader);

                    String Line;
                    String Parse = "";
                    while ((Line = i.readLine()) != null) {
                        Parse = Parse + Line;
                    }
                     System.out.println("Line to parse: "+Parse);

                    i.close();

                    } catch(Exception e) {
                        System.out.println("Parsing Error! ... "+e);
                    }
                    return ;
            }


        } catch (Exception e) {
            System.out.print("Error setting up and executing HTTP Request. Prices are not real time!");
        }

        return;
    }

    public void CancelTrade(CurrencyPair pair, int orderID) {
        Portfolio self = Portfolio.getSelf();

        //Example POST: https://api.binance.com/api/v3/order?symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559&signature=c8db56825ae71d6d79447849e617115f4a920fa2acdcab2b053c4b2838bd6b71
        String hardparams = "symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.05&recvWindow=5000&timestamp=1499827319559";

        String baseURL = "https://api.binance.com/api/v3/order?"; // order/test?
        String baseParams = "symbol="+pair.getPairName().toUpperCase()+"&orderId="+String.valueOf(orderID);
        String recWindow = "3000";
        String timestamp = ""+serverTime();
        String params = baseParams+"&recvWindow="+recWindow+"&timestamp="+timestamp;
        String signature = "null"; try { signature = encode(APISecret, params); } catch (Exception e) { System.err.println("Error signing Order Request! ... "+e); }
        String signedURLString = baseURL+params+"&signature="+signature;


        String hardRequest = "symbol=LTCBTC&side=BUY&type=LIMIT&timeInForce=GTC&quantity=1&price=0.1&recvWindow=5000&timestamp=1499827319559&signature="+signature;

        System.out.println(signedURLString);

        try {
            URL url = new URL(signedURLString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("DELETE");

            con.setRequestProperty("X-MBX-APIKEY", "NzlusZKEFHbIPCRh5xFwunOBCEcas1CFug9xfDFuVMb21200pKf43zSjRJYAW8RU");

            con.setDoOutput(true);
            //con.setDoInput(true);

            //OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            //wr.write();
            //wr.flush();

            con.connect();

            //Parse HTTP Response Code//
            int statusCode = con.getResponseCode();
            System.out.println(con.getResponseMessage());
            //System.out.println(con.getErrorStream());

            switch (statusCode) {

                //"Handle Successful Response"//
                case 200:
                    //System.out.println("Status 200! We're good to Parse! Output incoming!");
                    //Successful Connection, attempting to parse response!//
                    InputStreamReader inputReader = new InputStreamReader(con.getInputStream());
                    BufferedReader in = new BufferedReader(inputReader);

                    String tempLine;
                    String toParse = "";
                    while ((tempLine = in.readLine()) != null) {
                        toParse = tempLine;
                    }
                    try {
                        JSONObject jsonInput = new JSONObject(toParse);
                        System.out.println("Line to parse: "+toParse);

                    } catch(Exception e) {
                        System.out.println("Parsing Error! ... "+e);
                    }

                    in.close();
                    return ;

                default:
                    System.err.println("HTTP Response Error ... "+statusCode);
                    try {
                        InputStreamReader iReader = new InputStreamReader(con.getErrorStream());
                        BufferedReader i = new BufferedReader(iReader);

                        String Line;
                        String Parse = "";
                        while ((Line = i.readLine()) != null) {
                            Parse = Parse + Line;
                        }
                        System.out.println("Line to parse: "+Parse);

                        i.close();

                    } catch(Exception e) {
                        System.out.println("Parsing Error! ... "+e);
                    }
                    return ;
            }


        } catch (Exception e) {
            System.out.print("Error setting up and executing HTTP Request. Prices are not real time!");
        }

        return;
    }


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

    /* REST API Implementation : END */


    /* Websocket Implementation : BEGIN */

    public void logOrders(String tradeSource) {
        //System.out.println(tradeSource);

        //try {
            JSONObject tradeObject  = new JSONObject(tradeSource);
            String pairString = tradeObject.getString("s");
            CurrencyPair currPair = Portfolio.getSelf().binanceManager.fetchPairBySymbol(Portfolio.getSelf().binanceManager.activePairs, pairString.toLowerCase());

            JSONArray bidsList = tradeObject.getJSONArray("b");
            JSONArray asksList = tradeObject.getJSONArray("a");
            for (int i = 0; i < bidsList.length(); i++) {
                JSONArray bidArray = bidsList.getJSONArray(0);

                String bidPriceStr = bidArray.getString(0);
                String bidVolStr = bidArray.getString(1);

                double bidPrice = Double.parseDouble(bidPriceStr);
                double bidVol = Double.parseDouble(bidVolStr);

                LiveOrder bidUpdate = new LiveOrder(bidPrice, 1, bidVol);

                currPair.getDepth().updatePriceLevel(bidUpdate);

            } for (int t = 0; t < asksList.length(); t++) {
                JSONArray askArray = asksList.getJSONArray(0);

                String askPriceStr = askArray.getString(0);
                String askVolStr = askArray.getString(1);

                double askPrice = Double.parseDouble(askPriceStr);
                double askVol = Double.parseDouble(askVolStr);

                LiveOrder askUpdate = new LiveOrder(askPrice, 0, askVol);

                currPair.getDepth().updatePriceLevel(askUpdate);
            }

            currPair.getDepth().clearBlanks();
            currPair.getDepth().sortBooks();

            Portfolio.getSelf().binanceManager.TriangularArbitrage();

        //} catch (Exception e) {
        //    System.out.println("Error Handling Latest Data!");
        //    System.out.println("Error: " + e);
        //}
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
        //System.out.println("Message Received: " + message.getPayload());
        logOrders(message.getPayload());
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