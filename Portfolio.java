/**
 * Created by Trent Rand on 30/Sep/18.
 */
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Portfolio {

    public BinanceDelegate binanceDelegate = new BinanceDelegate();
    public BinanceUserDelegate binanceUserDelegate = new BinanceUserDelegate();
    public BinanceManager binanceManager = new BinanceManager();

    public String[] CurrencySymbols = {"btc", "eth", "bnb", "xrp", "ltc"};
    public ArrayList<Currency> Currencies = new ArrayList<>();

    public CSVOutput csvOutput = new CSVOutput();

    private int fileID = 0;

    private static Portfolio self = new Portfolio();
    public static Portfolio getSelf() {
        return self;
    }
    private Portfolio() {
        binanceDelegate = new BinanceDelegate();
        binanceUserDelegate = new BinanceUserDelegate();
        binanceManager = new BinanceManager();

        binanceManager.activeDelegate = binanceDelegate;

    }

    public static void main(String[] args) {
        for(int i = 0; i < 5; i++) {
            Currency tempCurrency = new Currency(self.CurrencySymbols[i]);
            self.Currencies.add(tempCurrency);
        }

        self.binanceUserDelegate.fetchUserStream(0);

        self.StayHot();
        self.Reporter();
        // Science Rulez!

        // Bill Nye The Science Guy.
        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(new StandardWebSocketClient(), self.binanceDelegate, "wss://stream.binance.com:9443/ws/xrpbtc@depth/xrpeth@depth/xrpbnb@depth/bnbbtc@depth/bnbeth@depth/ethbtc@depth");
        connectionManager.start();

        self.connectUserStream();

        self.userStream();

    }

    public void connectUserStream () {
        WebSocketConnectionManager userConnectionManager = new WebSocketConnectionManager(new StandardWebSocketClient(), self.binanceUserDelegate, "wss://stream.binance.com:9443/ws/"+self.binanceUserDelegate.getListenKey());
        userConnectionManager.start();

        //getSelf().userStream();
    }

    public void StayHot() {
        int delay = 200;   // delay for one minute.
        int period = 6000;  // repeat every god knows how long.

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                //waiting = false;
                getSelf().binanceManager.fetchOrderBooks();
                getSelf().binanceManager.cooldown = false;

            }
        }, delay, period);

    }

    public void userStream() {
        int delay = 60000;   // delay for one minute.
        int period = 900000;  // repeat every god knows how long.

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                //waiting = false;
                getSelf().binanceUserDelegate.fetchUserStream(1);
                getSelf().connectUserStream();
            }
        }, delay, period);

    }

    public void Reporter() {
        int delay = 200;   // delay for one minute.
        int period = 900000;  // repeat every god knows how long.

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                //waiting = false;
                getSelf().csvOutput.SaveSession(getSelf().fileID);
                getSelf().fileID++;
                System.out.println("Saving to file!");

            }
        }, delay, period);
    }

    public String fetchDateInString() {
        String toReturn = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
        return toReturn;
    }

    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
