/**
 * Created by Trent Rand on 30/Sep/18.
 */
public class CurrencyPair {

    public String pairName;
    public double Ask;
    public double Bid;

    public Currency currency1;
    public Currency currency2;

    public boolean isSynth;
    public Orderbook Depth;

    public LiveOrder activeBid;
    public LiveOrder activeAsk;

    public String getPairName() {
        return pairName;
    }

    public void setPairName(String pairName) {
        this.pairName = pairName;
    }

    public LiveOrder getAsk() {
        if(Depth != null) {
            return Depth.asks.get(0);
        } else { return null; }
    }

    public void setAsk(double ask) { this.Ask = ask; }

    public LiveOrder getBid() {
        if(Depth != null) {
            return Depth.bids.get(0);
        } else { return null; }
    }

    public double getBTCRelativeVolume(double volume, Currency currency, int inverse) {
        Double toReturn = 0.0;

        Portfolio self = Portfolio.getSelf();
        BinanceManager binanceManager = self.binanceManager;

        CurrencyPair btcPair = binanceManager.fetchPairBySymbol(self.binanceManager.activePairs, currency.getSymbol()+"btc");
        toReturn = volume * btcPair.getBid().getPrice();

        if (inverse > 0) {
            toReturn = volume / btcPair.getBid().getPrice();
        }

        return toReturn;
    }

    public void setBid(double bid) {
        Bid = bid;
    }

    public boolean isSynth() {
        return isSynth;
    }

    public void setSynth(boolean synth) {
        isSynth = synth;
    }

    public Orderbook getDepth() {
        return Depth;
    }

    public void setDepth(Orderbook depth) {
        Depth = depth;
    }

    @Override
    public String toString() {
        return "CurrencyPair{" +
                "pairName='" + pairName + '\'' +
                ", Ask=" + Ask +
                ", Bid=" + Bid +
                ", currency1=" + currency1 +
                ", currency2=" + currency2 +
                '}';
    }
}
