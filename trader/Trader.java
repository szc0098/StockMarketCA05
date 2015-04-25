package pkg.trader;

import java.util.ArrayList;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.order.BuyOrder;
import pkg.order.Order;
import pkg.order.OrderType;
import pkg.order.SellOrder;

public class Trader {
	String name;
	double cashInHand;
	ArrayList<Order> position;
	ArrayList<Order> ordersPlaced;

	public Trader(String name, double cashInHand) {
		super();
		this.name = name;
		this.cashInHand = cashInHand;
		this.position = new ArrayList<Order>();
		this.ordersPlaced = new ArrayList<Order>();
	}

	public void buyFromBank(Market m, String symbol, int volume)
			throws StockMarketExpection {
		double price = m.getStockForSymbol(symbol).getPrice();
		if(price * volume > this.cashInHand){
			throw new StockMarketExpection("Cannot place order for stock: " + symbol
					+ " since there is not enough money. Trader: " + this.name);
		}
		Order order = new BuyOrder(symbol, volume, price, this);
		this.position.add(order);
		this.cashInHand = this.cashInHand - price * volume;
	}

	public void placeNewOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		for(Order o : ordersPlaced){
	    	if(o.getStockSymbol().equals(symbol)){
	    		throw new StockMarketExpection(symbol + " is already placed.");
	    	}
	    }
		Order order;
		if(orderType == OrderType.BUY){
			if(price * volume > this.cashInHand){
				throw new StockMarketExpection("Cannot place order for stock: " + symbol
						+ " since there is not enough money. Trader: " + this.name);
			}
			order = new BuyOrder(symbol, volume, price, this);
			this.ordersPlaced.add(order);
			m.addOrder(order);
		}
		else{
			int bit = 0;
			for(Order o: position){
				if(o.getStockSymbol().equals(symbol)&&o.getSize()>=volume){
					bit = 1;
				}
			}
			if(bit == 0){
				throw new StockMarketExpection("You don't have the stock or enough stocks to sell. Trader: "+ this.name);
			}
			order = new SellOrder(symbol, volume, price, this);
			this.ordersPlaced.add(order);
			m.addOrder(order);
		}
	}

	public void placeNewMarketOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		for(Order o : ordersPlaced){
	    	if(o.getStockSymbol().equals(symbol)){
	    		throw new StockMarketExpection(symbol + " is already placed.");
	    	}
	    }
		Order order;
		if(orderType == OrderType.BUY){
			order = new BuyOrder(symbol, volume, true, this);
			this.ordersPlaced.add(order);
			m.addOrder(order);
		}
		else{
			int bit = 0;
			for(Order o: position){
				if(o.getStockSymbol().equals(symbol)&&o.getSize()>=volume){
					bit = 1;
				}
			}
			if(bit == 0){
				throw new StockMarketExpection("You don't have the stock or enough stocks to sell. Trader: " + this.name);
			}
			order = new SellOrder(symbol, volume, true, this);
			this.ordersPlaced.add(order);
			m.addOrder(order);
		}
	}

	public void tradePerformed(Order o, double matchPrice)
			throws StockMarketExpection {
		ordersPlaced.remove(o);
		if(o.getClass() == BuyOrder.class){
			this.cashInHand = this.cashInHand - matchPrice*o.getSize();
			this.position.add(o);
		}
		else{
			this.cashInHand = this.cashInHand + matchPrice*o.getSize();
            for(Order O: position){
            	if(O.getStockSymbol().equals(o.getStockSymbol())){
            		if(o.getSize() < O.getSize()){
            			O.setSize(O.getSize() - o.getSize());
            		}
            		else{
            			this.position.remove(O);
            		}
            		break;
            	}
            }
		}		
	}

	public void printTrader() {
		System.out.println("Trader Name: " + name);
		System.out.println("=====================");
		System.out.println("Cash: " + cashInHand);
		System.out.println("Stocks Owned: ");
		for (Order o : position) {
			o.printStockNameInOrder();
		}
		System.out.println("Stocks Desired: ");
		for (Order o : ordersPlaced) {
			o.printOrder();
		}
		System.out.println("+++++++++++++++++++++");
		System.out.println("+++++++++++++++++++++");
	}
}
