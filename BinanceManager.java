import javax.sound.sampled.Port;
import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by Trent Rand on 30/Sep/18.
 */

public class BinanceManager {

    //TODO: Move the following variables into the Portfolio.
    public double BNBbalance = 1.85;
    public double XRPbalance = 19;
    public double ETHbalance = 0.485;
    public double BTCbalance = 0.0031;
    public double LTCbalance = 0.20;

    public BinanceDelegate activeDelegate;
    public ArrayList<CurrencyPair> activePairs = new ArrayList<CurrencyPair>();
    public ArrayList<SyntheticPair> synthPairs = new ArrayList<SyntheticPair>();

    public String[] pairStrings = {"xrpbtc","xrpeth","xrpbnb","bnbbtc","bnbeth","ethbtc", "ltcbtc", "ltcbnb", "ltceth"};
    public String[] synthStrings = {"bnb-ETHBTC", "ltc-ETHBTC", "ltc-ETHBNB", "ltc-BNBBTC", "xrp-ETHBTC", "xrp-ETHBNB", "xrp-BNBBTC"};

    public boolean cooldown = false;

    public BinanceManager() {
        if (activeDelegate == null) { activeDelegate = new BinanceDelegate(); }

        setupPairs();
        fetchOrderBooks();

        try {
            //TriangularArbitrage();
        } catch (Exception e) {
            System.out.println("Trouble with init Triangulation! ... "+e);
        }
    }


    /* Triangular Arbitrage Algorithm : BEGIN */
    public void TriangularArbitrage() {
        Portfolio self = Portfolio.getSelf();
        double fwdGain = 1.0;
        double backGain = 1.0;
        double postGain = 0.0;

        double synthRatio;

        SyntheticPair currPair = null;

        //for(int z = 0; z < activePairs.size(); z++) {
        //    activePairs.get(z).getDepth().clearBlanks();
        //}
        if(!cooldown) {
            for (int i = 0; i < synthPairs.size(); i++) {
                currPair = synthPairs.get(i);
                String str = currPair.getPairName();
                CurrencyPair pairA = null;
                CurrencyPair pairB = null;
                CurrencyPair realPair = null;

                double volA;
                double volB;
                double volR;

                double maxVolA;
                double maxVolB;
                double maxVolR;

                double minVol;

                switch (str) {
                    case "bnb-ETHBTC":
                        pairA = fetchPairBySymbol(activePairs, "ethbtc");
                        pairB = fetchPairBySymbol(activePairs, "bnbbtc");
                        realPair = fetchPairBySymbol(activePairs, "bnbeth");

                        currPair.setPairA(pairA);
                        currPair.setPairB(pairB);
                        currPair.setRealPair(realPair);

                        fwdGain = currPair.getProjectedGain(1);
                        backGain = currPair.getProjectedGain(-1);

                        volA = pairA.getBid().getVolume(); //Avaliable BNB to SELL
                        volB = pairB.getAsk().getVolume(); //Avaliable BNB to BUY
                        volR = realPair.getBid().getVolume(); //Avaliable ETH to SELL

                        if (volA > BNBbalance) {
                            volA = BNBbalance;
                        }
                        if (volR > ETHbalance) {
                            volR = ETHbalance;
                        }
                        if (volB * pairB.getAsk().getPrice() > BTCbalance) {
                            volB = BTCbalance;
                        }

                        maxVolA = pairA.getBTCRelativeVolume(volA, pairA.currency1, 0);
                        maxVolB = pairB.getBTCRelativeVolume(volB, pairB.currency1, 0);
                        maxVolR = realPair.getBTCRelativeVolume(volR, realPair.currency1, 0);

                        minVol = Math.min((Math.min(maxVolA, maxVolB)), maxVolR);
                        minVol = realPair.getBTCRelativeVolume(minVol, realPair.currency1, 1);
                        BigDecimal BDvol = BigDecimal.valueOf(minVol);
                        BDvol = BDvol.setScale(3, BigDecimal.ROUND_FLOOR);

                        //minVol = self.round(minVol, 3);

                        String stringVolume = BDvol.toString();

                        stringVolume = "0.4";

                        //maxVolA = pairA.getBTCRelativeVolume(minVol, pairA.currency1, 1);
                        //maxVolB = pairB.getBTCRelativeVolume(minVol, pairB.currency1, 1);
                        //maxVolR = realPair.getBTCRelativeVolume(minVol, realPair.currency1, 1);

                        synthRatio = currPair.getSyntheticRatio(-1, 1);

                        BigDecimal highestBid = BigDecimal.valueOf(synthRatio * 0.99578);
                        highestBid = highestBid.setScale(6, BigDecimal.ROUND_FLOOR);
                        double highBid = highestBid.doubleValue();
                        String strBid = highestBid.toString();


                        BigDecimal lowestAsk = BigDecimal.valueOf(synthRatio * 1.00422);
                        lowestAsk = lowestAsk.setScale(6, BigDecimal.ROUND_FLOOR);
                        double lowAsk = lowestAsk.doubleValue();
                        String strAsk = lowestAsk.toString();

                        if (realPair.activeAsk == null) {
                            activeDelegate.ExecuteTrade(realPair, "SELL", lowestAsk.doubleValue(), stringVolume);
                        } else if (realPair.activeAsk.getPrice() > lowAsk || realPair.activeAsk.getPrice() < lowAsk * 0.999) {
                            activeDelegate.CancelTrade(realPair, realPair.activeAsk.orderID);
                            activeDelegate.ExecuteTrade(realPair, "SELL", lowestAsk.doubleValue(), stringVolume);
                        }

                        if (realPair.activeBid == null) {
                            activeDelegate.ExecuteTrade(realPair, "BUY", highestBid.doubleValue(), stringVolume);
                        } else if (realPair.activeBid.getPrice() < highBid || realPair.activeBid.getPrice() > highBid * 1.001) {
                            activeDelegate.CancelTrade(realPair, realPair.activeBid.orderID);
                            activeDelegate.ExecuteTrade(realPair, "BUY", highestBid.doubleValue(), stringVolume);
                        }


                        break;

                    case "ltc-ETHBTC":
                        pairA = fetchPairBySymbol(activePairs, "ltceth");
                        pairB = fetchPairBySymbol(activePairs, "ltcbtc");
                        realPair = fetchPairBySymbol(activePairs, "ethbtc");

                        currPair.setPairA(pairA);
                        currPair.setPairB(pairB);
                        currPair.setRealPair(realPair);

                        fwdGain = currPair.getProjectedGain(1);
                        backGain = currPair.getProjectedGain(-1);


                        volA = pairA.getBid().getVolume();
                        volB = pairB.getAsk().getVolume();
                        volR = realPair.getBid().getVolume(); //Avaliable ETH to SELL

                        if (volA > LTCbalance) {
                            volA = LTCbalance;
                        }
                        if (volR > ETHbalance) {
                            volR = ETHbalance;
                        }
                        if (volB * pairB.getAsk().getPrice() > BTCbalance) {
                            volB = BTCbalance;
                        }

                        maxVolA = pairA.getBTCRelativeVolume(volA, pairA.currency1, 0);
                        maxVolB = pairB.getBTCRelativeVolume(volB, pairB.currency1, 0);
                        maxVolR = realPair.getBTCRelativeVolume(volR, realPair.currency1, 0);

                        minVol = Math.min((Math.min(maxVolA, maxVolB)), maxVolR);

                        maxVolA = pairA.getBTCRelativeVolume(minVol, pairA.currency1, 1);
                        maxVolB = pairB.getBTCRelativeVolume(minVol, pairB.currency1, 1);
                        maxVolR = realPair.getBTCRelativeVolume(minVol, realPair.currency1, 1);


                        if (fwdGain > 0.23) {
                            postGain = fwdGain - 0.225;

                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(fwdGain), String.valueOf(postGain));
                            //activeDelegate.ExecuteTrade(pairA, "SELL", pairA.getBid().getPrice(), volA);
                            //activeDelegate.ExecuteTrade(realPair, "SELL", realPair.getBid().getPrice(), volR);
                            //activeDelegate.ExecuteTrade(pairB, "BUY", pairB.getAsk().getPrice(), volB);

                        } else if (backGain > 0.23) {
                            postGain = backGain - 0.225;

                            System.out.println("Reuqires Back Trade!");
                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(backGain), String.valueOf(postGain));


                        }

                        break;
                    case "ltc-ETHBNB":
                        pairA = fetchPairBySymbol(activePairs, "ltcbnb");
                        pairB = fetchPairBySymbol(activePairs, "ltceth");
                        realPair = fetchPairBySymbol(activePairs, "bnbeth");

                        currPair.setPairA(pairA);
                        currPair.setPairB(pairB);
                        currPair.setRealPair(realPair);

                        fwdGain = currPair.getProjectedGain(1);
                        backGain = currPair.getProjectedGain(-1);


                        volA = pairA.getBid().getVolume();
                        volB = pairB.getAsk().getVolume();
                        volR = realPair.getBid().getVolume(); //Avaliable ETH to SELL

                        if (volA > LTCbalance) {
                            volA = LTCbalance;
                        }
                        if (volR > BNBbalance) {
                            volR = BNBbalance;
                        }
                        if (volB * pairB.getAsk().getPrice() > ETHbalance) {
                            volB = ETHbalance;
                        }
                        maxVolA = pairA.getBTCRelativeVolume(volA, pairA.currency1, 0);
                        maxVolB = pairB.getBTCRelativeVolume(volB, pairB.currency1, 0);
                        maxVolR = realPair.getBTCRelativeVolume(volR, realPair.currency1, 0);

                        minVol = Math.min((Math.min(maxVolA, maxVolB)), maxVolR);

                        maxVolA = pairA.getBTCRelativeVolume(minVol, pairA.currency1, 1);
                        maxVolB = pairB.getBTCRelativeVolume(minVol, pairB.currency1, 1);
                        maxVolR = realPair.getBTCRelativeVolume(minVol, realPair.currency1, 1);


                        if (fwdGain > 0.23) {
                            postGain = fwdGain - 0.225;

                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(fwdGain), String.valueOf(postGain));
                            //activeDelegate.ExecuteTrade(pairA, "SELL", pairA.getBid().getPrice(), volA);
                            //activeDelegate.ExecuteTrade(realPair, "SELL", realPair.getBid().getPrice(), volR);
                            //activeDelegate.ExecuteTrade(pairB, "BUY", pairB.getAsk().getPrice(), volB);

                        } else if (backGain > 0.23) {
                            postGain = backGain - 0.225;

                            System.out.println("Requires Back Trade!");
                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(backGain), String.valueOf(postGain));


                        }

                        break;
                    case "ltc-BNBBTC":
                        pairA = fetchPairBySymbol(activePairs, "ltcbnb");
                        pairB = fetchPairBySymbol(activePairs, "ltcbtc");
                        realPair = fetchPairBySymbol(activePairs, "bnbbtc");

                        currPair.setPairA(pairA);
                        currPair.setPairB(pairB);
                        currPair.setRealPair(realPair);

                        fwdGain = currPair.getProjectedGain(1);
                        backGain = currPair.getProjectedGain(-1);


                        volA = pairA.getBid().getVolume();
                        volB = pairB.getAsk().getVolume();
                        volR = realPair.getBid().getVolume(); //Avaliable ETH to SELL

                        if (volA > LTCbalance) {
                            volA = LTCbalance;
                        }
                        if (volR > BNBbalance) {
                            volR = BNBbalance;
                        }
                        if (volB * pairB.getAsk().getPrice() > BTCbalance) {
                            volB = BTCbalance;
                        }

                        maxVolA = pairA.getBTCRelativeVolume(volA, pairA.currency1, 0);
                        maxVolB = pairB.getBTCRelativeVolume(volB, pairB.currency1, 0);
                        maxVolR = realPair.getBTCRelativeVolume(volR, realPair.currency1, 0);

                        minVol = Math.min((Math.min(maxVolA, maxVolB)), maxVolR);

                        maxVolA = pairA.getBTCRelativeVolume(minVol, pairA.currency1, 1);
                        maxVolB = pairB.getBTCRelativeVolume(minVol, pairB.currency1, 1);
                        maxVolR = realPair.getBTCRelativeVolume(minVol, realPair.currency1, 1);


                        if (fwdGain > 0.23) {
                            postGain = fwdGain - 0.225;

                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(fwdGain), String.valueOf(postGain));
                            //activeDelegate.ExecuteTrade(pairA, "SELL", pairA.getBid().getPrice(), volA);
                            //activeDelegate.ExecuteTrade(realPair, "SELL", realPair.getBid().getPrice(), volR);
                            //activeDelegate.ExecuteTrade(pairB, "BUY", pairB.getAsk().getPrice(), volB);

                        } else if (backGain > 0.23) {
                            postGain = backGain - 0.225;

                            System.out.println("Reuqires Back Trade!");
                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(backGain), String.valueOf(postGain));


                        }

                        break;
                    case "xrp-ETHBTC":
                        pairA = fetchPairBySymbol(activePairs, "xrpeth");
                        pairB = fetchPairBySymbol(activePairs, "xrpbtc");
                        realPair = fetchPairBySymbol(activePairs, "ethbtc");

                        currPair.setPairA(pairA);
                        currPair.setPairB(pairB);
                        currPair.setRealPair(realPair);

                        fwdGain = currPair.getProjectedGain(1);
                        backGain = currPair.getProjectedGain(-1);

                        volA = pairA.getBid().getVolume();
                        volB = pairB.getAsk().getVolume();
                        volR = realPair.getBid().getVolume(); //Avaliable ETH to SELL

                        if (volA > XRPbalance) {
                            volA = XRPbalance;
                        }
                        if (volR > ETHbalance) {
                            volR = ETHbalance;
                        }
                        if (volB * pairB.getAsk().getPrice() > BTCbalance) {
                            volB = BTCbalance;
                        }

                        maxVolA = pairA.getBTCRelativeVolume(volA, pairA.currency1, 0);
                        maxVolB = pairB.getBTCRelativeVolume(volB, pairB.currency1, 0);
                        maxVolR = realPair.getBTCRelativeVolume(volR, realPair.currency1, 0);

                        minVol = Math.min((Math.min(maxVolA, maxVolB)), maxVolR);

                        maxVolA = pairA.getBTCRelativeVolume(minVol, pairA.currency1, 1);
                        maxVolB = pairB.getBTCRelativeVolume(minVol, pairB.currency1, 1);
                        maxVolR = realPair.getBTCRelativeVolume(minVol, realPair.currency1, 1);


                        if (fwdGain > 0.23) {
                            postGain = fwdGain - 0.225;

                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(fwdGain), String.valueOf(postGain));
                            //activeDelegate.ExecuteTrade(pairA, "SELL", pairA.getBid().getPrice(), volA);
                            //activeDelegate.ExecuteTrade(realPair, "SELL", realPair.getBid().getPrice(), volR);
                            //activeDelegate.ExecuteTrade(pairB, "BUY", pairB.getAsk().getPrice(), volB);

                        } else if (backGain > 0.23) {
                            postGain = backGain - 0.225;

                            System.out.println("Reuqires Back Trade!");
                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(backGain), String.valueOf(postGain));


                        }

                        break;
                    case "xrp-BNBBTC":
                        pairA = fetchPairBySymbol(activePairs, "xrpbnb");
                        pairB = fetchPairBySymbol(activePairs, "xrpbtc");
                        realPair = fetchPairBySymbol(activePairs, "bnbbtc");

                        currPair.setPairA(pairA);
                        currPair.setPairB(pairB);
                        currPair.setRealPair(realPair);

                        fwdGain = currPair.getProjectedGain(1);
                        backGain = currPair.getProjectedGain(-1);


                        volA = pairA.getBid().getVolume();
                        volB = pairB.getAsk().getVolume();
                        volR = realPair.getBid().getVolume(); //Avaliable ETH to SELL

                        if (volA > XRPbalance) {
                            volA = XRPbalance;
                        }
                        if (volR > BNBbalance) {
                            volR = BNBbalance;
                        }
                        if (volB * pairB.getAsk().getPrice() > BTCbalance) {
                            volB = BTCbalance;
                        }

                        maxVolA = pairA.getBTCRelativeVolume(volA, pairA.currency1, 0);
                        maxVolB = pairB.getBTCRelativeVolume(volB, pairB.currency1, 0);
                        maxVolR = realPair.getBTCRelativeVolume(volR, realPair.currency1, 0);

                        minVol = Math.min((Math.min(maxVolA, maxVolB)), maxVolR);

                        maxVolA = pairA.getBTCRelativeVolume(minVol, pairA.currency1, 1);
                        maxVolB = pairB.getBTCRelativeVolume(minVol, pairB.currency1, 1);
                        maxVolR = realPair.getBTCRelativeVolume(minVol, realPair.currency1, 1);


                        if (fwdGain > 0.23) {
                            postGain = fwdGain - 0.225;

                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(fwdGain), String.valueOf(postGain));
                            //activeDelegate.ExecuteTrade(pairA, "SELL", pairA.getBid().getPrice(), volA);
                            //activeDelegate.ExecuteTrade(realPair, "SELL", realPair.getBid().getPrice(), volR);
                            //activeDelegate.ExecuteTrade(pairB, "BUY", pairB.getAsk().getPrice(), volB);

                        } else if (backGain > 0.23) {
                            postGain = backGain - 0.225;

                            System.out.println("Requires Back Trade! "+backGain);
                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(backGain), String.valueOf(postGain));


                        }
                        break;
                    case "xrp-ETHBNB":
                        pairA = fetchPairBySymbol(activePairs, "xrpbnb");
                        pairB = fetchPairBySymbol(activePairs, "xrpeth");
                        realPair = fetchPairBySymbol(activePairs, "bnbeth");

                        currPair.setPairA(pairA);
                        currPair.setPairB(pairB);
                        currPair.setRealPair(realPair);

                        fwdGain = currPair.getProjectedGain(1);
                        backGain = currPair.getProjectedGain(-1);


                        volA = pairA.getBid().getVolume();
                        volB = pairB.getAsk().getVolume();
                        volR = realPair.getBid().getVolume(); //Avaliable ETH to SELL

                        if (volA > XRPbalance) {
                            volA = XRPbalance;
                        }
                        if (volR > BNBbalance) {
                            volR = BNBbalance;
                        }
                        if (volB * pairB.getAsk().getPrice() > ETHbalance) {
                            volB = ETHbalance;
                        }

                        maxVolA = pairA.getBTCRelativeVolume(volA, pairA.currency1, 0);
                        maxVolB = pairB.getBTCRelativeVolume(volB, pairB.currency1, 0);
                        maxVolR = realPair.getBTCRelativeVolume(volR, realPair.currency1, 0);

                        minVol = Math.min((Math.min(maxVolA, maxVolB)), maxVolR);

                        maxVolA = pairA.getBTCRelativeVolume(minVol, pairA.currency1, 1);
                        maxVolB = pairB.getBTCRelativeVolume(minVol, pairB.currency1, 1);
                        maxVolR = realPair.getBTCRelativeVolume(minVol, realPair.currency1, 1);

                        minVol = Math.floor(minVol);


                        if (fwdGain > 0.23) {
                            postGain = fwdGain - 0.225;

                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(fwdGain), String.valueOf(postGain));
                            //activeDelegate.ExecuteTrade(pairA, "SELL", pairA.getBid().getPrice(), volA);
                            //activeDelegate.ExecuteTrade(realPair, "SELL", realPair.getBid().getPrice(), volR);
                            //activeDelegate.ExecuteTrade(pairB, "BUY", pairB.getAsk().getPrice(), volB);

                        } else if (backGain > 0.23) {
                            postGain = backGain - 0.225;
                            System.out.println("Reuqires Back Trade!");
                            logOpportunity(currPair, realPair, minVol, self.fetchDateInString(), String.valueOf(backGain), String.valueOf(postGain));

                        }
                        break;

                    default:


                        break;
                }

                System.out.println("");
            }
        }

    }


    private void logOpportunity(SyntheticPair synthPair, CurrencyPair realPair, double minVolume, String date, String gain, String postGain) {
            Portfolio self = Portfolio.getSelf();

        if (!cooldown) {
            switch (synthPair.getPairName()) {
                case "xrp-ETHBTC":
                    System.err.println("Initial XRP Balance: " + XRPbalance);

                    XRPbalance = XRPbalance * (1 + Double.parseDouble(postGain)/100);

                    System.err.println("Final XRP Balance: " + XRPbalance);

                    self.csvOutput.addToOutput("XRP", synthPair.getPairName(), postGain, String.valueOf(XRPbalance), self.fetchDateInString(), minVolume);



                    cooldown = true;

                    break;
                case "bnb-ETHBTC":
                    System.err.println("Initial BNB Balance: " + BNBbalance);


                    BNBbalance = BNBbalance * (1 + Double.parseDouble(postGain)/100);

                    System.err.println("Final BNB Balance: " + BNBbalance);

                    self.csvOutput.addToOutput("BNB", synthPair.getPairName(), String.valueOf(postGain), String.valueOf(BNBbalance), self.fetchDateInString(), minVolume);
                    cooldown = true;

                    break;
                case "ltc-ETHBTC":
                    System.err.println("Initial LTC Balance: " + LTCbalance);


                    LTCbalance = LTCbalance * (1 + Double.parseDouble(postGain)/100);

                    System.err.println("Final LTC Balance: " + LTCbalance);

                    self.csvOutput.addToOutput("LTC", synthPair.getPairName(), String.valueOf(postGain), String.valueOf(LTCbalance), self.fetchDateInString(), minVolume);
                    cooldown = true;

                    break;
                case "eth-LTCBNB":
                    System.err.println("Initial ETH Balance: " + ETHbalance);


                    ETHbalance = ETHbalance * (1 + Double.parseDouble(postGain)/100);

                    System.err.println("Final ETH Balance: " + ETHbalance);

                    self.csvOutput.addToOutput("ETH", synthPair.getPairName(), String.valueOf(postGain), String.valueOf(ETHbalance), self.fetchDateInString(), minVolume);
                    cooldown = true;

                    break;
                case "eth-XRPBNB":
                    System.err.println("Initial ETH Balance: " + ETHbalance);

                    ETHbalance = ETHbalance * (1 + Double.parseDouble(postGain)/100);

                    System.err.println("Final ETH Balance: " + ETHbalance);

                    self.csvOutput.addToOutput("ETH", synthPair.getPairName(), String.valueOf(postGain), String.valueOf(ETHbalance), self.fetchDateInString(), minVolume);
                    cooldown = true;

                    break;
                case "xrp-BNBBTC":
                    System.err.println("Initial XRP Balance: " + XRPbalance);

                    XRPbalance = XRPbalance * (1 + Double.parseDouble(postGain)/100);

                    System.err.println("Final XRP Balance: " + XRPbalance);

                    self.csvOutput.addToOutput("XRP", synthPair.getPairName(), String.valueOf(postGain), String.valueOf(XRPbalance), self.fetchDateInString(), minVolume);
                    cooldown = true;

                    break;
                case "ltc-BNBBTC":
                    System.err.println("Initial LTC Balance: " + LTCbalance);

                    LTCbalance = LTCbalance * (1 + Double.parseDouble(postGain)/100);

                    System.err.println("Final LTC Balance: " + LTCbalance);

                    self.csvOutput.addToOutput("LTC", synthPair.getPairName(), String.valueOf(postGain), String.valueOf(LTCbalance), self.fetchDateInString(), minVolume);
                    cooldown = true;

                    break;
                default:


                    break;
            }
        }

            System.out.println(synthPair.pairName+" Opportunity Found and Logged! "+postGain);
            self.csvOutput.addToOutput(synthPair.pairName,String.valueOf(realPair.getAsk()),String.valueOf(synthPair.syntheticRatio),gain,postGain,date);
    }


    public void dropOrders(CurrencyPair currencyPair) {



    }



    /* Triangular Arbitrage Algorithm : END */


    /* Init Functions : BEGIN */

    public void fetchOrderBooks() {
        for(int indexz = 0; indexz < activePairs.size(); indexz++) {
            activeDelegate.getOrderBook(activePairs.get(indexz));
        }
    }

    private void setupPairs() {
        for(int index = 0; index < pairStrings.length; index++) {
            CurrencyPair tempPair = new CurrencyPair();
            tempPair.setPairName(pairStrings[index]);
            Currency curr1 = new Currency(pairStrings[index].substring(0,3)); //May be 0,3 so check here on error!
            Currency curr2 = new Currency(pairStrings[index].substring(3,6)); //May be 4,6 so check here on error!
            tempPair.currency1 = curr1;
            tempPair.currency2 = curr2;
            activePairs.add(tempPair);
        }
        for(int index = 0; index < synthStrings.length; index++) {
            SyntheticPair tempPair = new SyntheticPair();
            tempPair.setPairName(synthStrings[index]);
            synthPairs.add(tempPair);
        }
    }

    public void tradeFilled(String symbol, String side, String stringPrice) {
        CurrencyPair filledPair = fetchPairBySymbol(activePairs, symbol);
        SyntheticPair synthPair = null;

        for(int i = 0; i < synthPairs.size(); i++) {
            if(synthPairs.get(i).getRealPair() == filledPair) {
                synthPair = synthPairs.get(i);
            }
        }

        try {
            String toWriteOut = synthPair.getPairA().getAsk().getPrice() + "," + synthPair.getPairA().getBid().getPrice() + "," +
                    synthPair.getPairB().getAsk().getPrice() + "," + synthPair.getPairB().getBid().getPrice() + "," +
                    stringPrice + "," + side;
            Portfolio.getSelf().csvOutput.tradeLog = toWriteOut;
            Portfolio.getSelf().csvOutput.SaveSession(17);

        } catch (Exception e) { System.err.println("Error writing filled trade result!"); }
    }

    /* Init Functions : END */

    public CurrencyPair fetchPairBySymbol(ArrayList<CurrencyPair> pairs, String symbol) {
        CurrencyPair toReturn = null;
        for(CurrencyPair pair : pairs) {
            if(pair.getPairName().equals(symbol)) {
                toReturn = pair;
            }
        }
        return toReturn;
    }

}
