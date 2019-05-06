import java.util.ArrayList;
import java.util.Collections;

import java.util.*;

/**
 * Created by Trent Rand on 30/Sep/18.
 */
public class Orderbook implements Comparator<LiveOrder> {

    public ArrayList<LiveOrder> bids = new ArrayList<>();
    public ArrayList<LiveOrder> asks = new ArrayList<>();
    CurrencyPair pair;

    public void sortBooks() {
        Collections.sort(bids, this);
        Collections.reverse(bids);
        Collections.sort(asks, this);

        //if(pair != null) { System.out.println(""+pair.getPairName()); }


        //System.out.println(" Highest Bid ... ? "+bids.get(0).getPrice());
        //System.out.println(" Lowest Ask ... ? "+asks.get(0).getPrice());

    }

    public void updatePriceLevel(LiveOrder priceLevel) {
            if (priceLevel.getSide() == 1) {
                updateVolume(bids, priceLevel.price, priceLevel.volume, 1);
            } else if (priceLevel.getSide() == 0) {
                updateVolume(asks, priceLevel.price, priceLevel.volume, 0);
            }
    }

    public void clearBlanks() {
        // Garbage Collection //
        for(int z = 0; z < bids.size(); z++) {
            if(bids.get(z).getVolume() == 0) {
                bids.remove(z);
            }
        } for(int x = 0; x < asks.size(); x++) {
            if(asks.get(x).getVolume() == 0) {
                asks.remove(x);
            }
        }
    }

    private void updateVolume(ArrayList<LiveOrder> depth, double priceToUpdate, double newVolume, int side) {
        for(LiveOrder order : depth) {
            if(order.getPrice() == priceToUpdate) {
                int indexToUpdate = depth.indexOf(order);
                depth.get(indexToUpdate).setVolume(newVolume);
                return;
            }
        }

        LiveOrder toAdd = new LiveOrder (priceToUpdate, side, newVolume);
        depth.add(toAdd);
        sortBooks();
    }



    public int compare(LiveOrder a, LiveOrder b) {
        if (a.getPrice() - b.getPrice() > 0) {
            return 1;
        } else if (a.getPrice() - b.getPrice() < 0) {
            return -1;
        } else {
            return 0;
        }
    }

}
