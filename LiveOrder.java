public class LiveOrder {
    public double price;
    public int side; //0 == SELL, 1 == BUY. Throw error on else if possible.
    public double volume;

    public int orderID;

    public LiveOrder(double initPrice, int initSide, double initVol) {
        price = initPrice;
        side = initSide;
        volume = initVol;
    }

    public LiveOrder(double initprice, String strSide, double vol, int orderId) {
        price = initprice;
        if(strSide == "SELL") {
            side = 0;
        } else if (strSide == "BUY") {
            side = 1;
        }
        volume = vol;
        orderID = orderId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSide() {
        return side;
    }

    public void setSide(int side) {
        this.side = side;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }
}
