import java.io.FileWriter;

/**
 * Created by Trent Rand on 09/Feb/18.
 */
public class CSVOutput {

    String sessionLog = "PairName,RealRatio,SynthRatio,PercentOpportunity,PercentGain,Date,\n";
    String tradeLog = "null";

    public void addToOutput(String pairName, String realRatio, String synthRatio, String percentOpportunity, String percentGain, String date) {
        if(sessionLog == "null") {
            sessionLog = "PairName,RealRatio,SynthRatio,PercentOpportunity,PercentGain,Date,\n";
        }
                sessionLog = sessionLog + pairName + "," + realRatio + "," + synthRatio + "," + percentOpportunity + "," + percentGain + "," + date + ",\n";
                System.out.println("Session Log Updated!");
                //System.out.println(sessionLog);
    }

    public void addToOutput(String currency, String pairName, String percentGain, String endingBalance, String date, double volLimit) {
        if(tradeLog == "null") {
            tradeLog = "Currency,SynthPair,PercentGain,EndingBalance,Date\n";
        }

        tradeLog = tradeLog + currency + "," + pairName + "," + percentGain + "," + endingBalance + "," + date + "," + volLimit + ",\n";
        System.out.println("Trade Log Updated!");
        //System.out.println(sessionLog);
    }

    public void SaveSession(int fileID) {
        try {
            FileWriter writer = new FileWriter("Session"+fileID+".csv");
            writer.append(sessionLog.subSequence(0, sessionLog.length()));

            writer.flush();
            writer.close();

            writer = new FileWriter("Trade"+fileID+".csv");
            writer.append(tradeLog.subSequence(0, tradeLog.length()));

            writer.flush();
            writer.close();

            sessionLog = "null";
        } catch(Exception e) {
            System.out.print("Error saving the output file!\n");
        }
    }





}