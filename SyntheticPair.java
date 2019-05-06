/**
 * Created by Trent Rand on 30/Sep/18.
 */
public class SyntheticPair extends CurrencyPair {

    public CurrencyPair pairA;
    public CurrencyPair pairB;
    public CurrencyPair realPair;

    public double syntheticRatio;

    public double getSyntheticRatio(int sideA, int sideB) {
        double priceA;
        double priceB;
        if (sideA > 0) { priceA = pairA.getAsk().getPrice();
        } else { priceA = pairA.getBid().getPrice(); }
        if (sideB > 0) { priceB = pairB.getAsk().getPrice();
        } else { priceB = pairB.getBid().getPrice(); }

        priceA = 1 / priceA;
        priceB = 1 / priceB;

        syntheticRatio = priceA / priceB;

        return syntheticRatio;
    }

    public double getProjectedGain(int dir) {
        double toReturn = 0;
        double initBal = 100;

        if(dir > 0) {
            initBal = initBal * pairA.getBid().getPrice();
            initBal = initBal * realPair.getBid().getPrice();
            initBal = initBal / pairB.getAsk().getPrice();

            toReturn = initBal - 100;
        } else if (dir < 0) {
            initBal = initBal * pairB.getBid().getPrice();
            initBal = initBal / realPair.getAsk().getPrice();
            initBal = initBal / pairA.getAsk().getPrice();

            toReturn = initBal - 100;
        }

        System.out.println(getPairName()+" Potential P/L% : "+toReturn);

        return toReturn;
    }

    public CurrencyPair getPairA() {
        return pairA;
    }

    public void setPairA(CurrencyPair pairA) {
        this.pairA = pairA;
    }

    public CurrencyPair getPairB() {
        return pairB;
    }

    public void setPairB(CurrencyPair pairB) {
        this.pairB = pairB;
    }

    public CurrencyPair getRealPair() {
        return realPair;
    }

    public void setRealPair(CurrencyPair realPair) {
        this.realPair = realPair;
    }
}
