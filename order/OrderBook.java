package pkg.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.text.html.MinimalHTMLWriter;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.market.api.PriceSetter;

public class OrderBook {
	Market market;
	HashMap<String, ArrayList<Order>> buyOrders;
	HashMap<String, ArrayList<Order>> sellOrders;

	public OrderBook(Market m) {
		this.market = m;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();
	}

	public void addToOrderBook(Order order) {

		if (order.getClass() == BuyOrder.class) {
			addNewOrders(order,buyOrders);
		} else {
			addNewOrders(order,sellOrders);
		}
	}

	
	
	public void tradeOrders() throws StockMarketExpection {
		
		PriceSetter priceSetter = new PriceSetter();
		Double [] price = new Double[40];
		HashMap<Double, Integer> bOrder = new HashMap<Double, Integer>();
		HashMap<Double, Integer> sOrder = new HashMap<Double, Integer>();
		ArrayList<Order> allBuyOrders;
		ArrayList<Order> allSellOrders;
		ArrayList<String> stockList = new ArrayList<String>();
		int priceIndex = 0;
		Set set = buyOrders.entrySet();
        addToStockList(set, stockList);
        
        set = sellOrders.entrySet();
        addToStockList(set, stockList);
        
        for (String n: stockList){
        	allBuyOrders = buyOrders.get(n);
        	allSellOrders = sellOrders.get(n);
        	priceIndex = 0;
        	int marketBuyOrder = 0;
        	for (Order buyOrder: allBuyOrders){
        		if(buyOrder.isMarketOrder == true){
        			marketBuyOrder = buyOrder.getSize();
        		}
        		else{
        		price[priceIndex] = buyOrder.getPrice();
        		priceIndex++;
        		}
        	}
        	
        	int marketSellOrder = 0;
        	double dupPrice=0;
        	int flagForDuplicatePrice = 0;
        	for (Order sellOrder : allSellOrders){
        		if(sellOrder.isMarketOrder == true){
        			marketSellOrder = sellOrder.getSize();
        		}
        		else{
        		dupPrice = sellOrder.getPrice();
        		flagForDuplicatePrice = checkForDuplicateOrderPrice(price,priceIndex, dupPrice, flagForDuplicatePrice);
        		if(flagForDuplicatePrice == 0){
        			price[priceIndex]=dupPrice;
        			priceIndex++;
        			}
        		}
        	}
        	
        	sortOrders(price, priceIndex);
			calculateMatchPriceForBuyOrder(price, bOrder, allBuyOrders,
					priceIndex, marketBuyOrder);
        	calculateMatchPriceForSellOrders(price, sOrder, allSellOrders,
					priceIndex, marketSellOrder);
        	
        	 
        	 double matchingPrice = 0;
        	 int minVol = 0;
        	 int maxVol = 0;
        	 int bVol =0 ;
        	 int sVol =  0;
        	 for (int x= 0; x<= priceIndex-1; x++){ 
        		 bVol = bOrder.get(price[x]);
        		 sVol = sOrder.get(price[x]);
        	
        	minVol = Math.min(bVol, sVol);
        	if(maxVol <= minVol && minVol != 0){
        		maxVol = minVol;
        		matchingPrice = price[x];
        	}
        	 }
        	 setNewMatchPrice(priceSetter, allBuyOrders, allSellOrders, n,
					matchingPrice, maxVol); 	
        }

		    }

	
	
	private int checkForDuplicateOrderPrice(Double[] price, int priceIndex,
			double dupPrice, int flagForDuplicatePrice) {
		for(int x=0; x< priceIndex ; x++){
			if(price[x] == dupPrice){
				flagForDuplicatePrice = 1;
			}
		}
		return flagForDuplicatePrice;
	}

	private void setNewMatchPrice(PriceSetter priceSetter,
			ArrayList<Order> allBuyOrders, ArrayList<Order> allSellOrders,
			String n, double matchingPrice, int maxVol)
			throws StockMarketExpection {
		ArrayList<Order> copy = (ArrayList<Order>)allBuyOrders.clone();
		 calculateShiftInOrdersAfterTrade(allBuyOrders, matchingPrice,maxVol, copy);
			
		 copy = (ArrayList<Order>)allSellOrders.clone();
		 calculateShiftInOrdersAfterTrade(allSellOrders, matchingPrice,maxVol, copy);
			
			
		market.getMarketHistory().setSubject(priceSetter);
		priceSetter.registerObserver(market.getMarketHistory());
		priceSetter.setNewPrice(market, n, matchingPrice);
	}

	private void sortOrders(Double[] price, int priceIndex) {
		double tempPriceValue =0;
		for (int y=0; y<priceIndex-1;y++){
			for (int x = 0; x < priceIndex-1-y; x++){
			if(price[x]>price[x+1]){
				tempPriceValue = price[x];
				price[x]=price[x+1];
				price[x+1]= tempPriceValue;
				
			}
			}
		}
	}

	private void calculateMatchPriceForSellOrders(Double[] price,
			HashMap<Double, Integer> sOrder, ArrayList<Order> allSellOrders,
			int priceIndex, int marketSellOrder) {
		int flagForMatchPrice;
		int value= marketSellOrder;
		for ( int x = 0 ; x <= priceIndex-1 ; x++){
			
			flagForMatchPrice = 0;
			for(Order o: allSellOrders){
				flagForMatchPrice = calculateOrderMatchPrice(price, sOrder,flagForMatchPrice, value, x, o);
			}
				if(flagForMatchPrice == 0){
					sOrder.put(price[x], 0);
				}    		
		}
	}

	private void calculateMatchPriceForBuyOrder(Double[] price,
			HashMap<Double, Integer> bOrder, ArrayList<Order> allBuyOrders,
			int priceIndex, int marketBuyOrder) {
		int flagForMatchPrice = 0;
		int value = marketBuyOrder;
		for ( int x = priceIndex-1 ; x >=0 ; x--){
			for(Order o: allBuyOrders){
				flagForMatchPrice = calculateOrderMatchPrice(price, bOrder,flagForMatchPrice, value, x, o);
			}
				if(flagForMatchPrice == 0){
					bOrder.put(price[x], 0);
				}
			
		}
	}

	private void calculateShiftInOrdersAfterTrade(
			ArrayList<Order> allBuyOrders, double matchingPrice, int maxVol, ArrayList<Order> copy)
			throws StockMarketExpection {
		 int shift = 0;
   	     int pool = 0;
		for(Order o: copy){
			if(o.getPrice() >= matchingPrice||o.isMarketOrder() == true){
				if(pool == maxVol){
					break;
				}
				if(o.getSize() + pool - maxVol > 0){
					shift = o.getSize() + pool - maxVol;
				}
				o.setSize(o.getSize() - shift);
				pool = pool + o.getSize();
				o.getTrader().tradePerformed(o, matchingPrice);
				allBuyOrders.remove(o);
			}
		}
	}

	private int calculateOrderMatchPrice(Double[] price,
			HashMap<Double, Integer> bOrder, int flagForMatchPrice, int value,
			int x, Order o) {
		if (o.getPrice() == price[x]){
			value = value + o.getSize();
			bOrder.put(price[x], value);
			flagForMatchPrice = 1;
		}
		return flagForMatchPrice;
	}

	private void addToStockList(Set set, ArrayList<String> stockList) {
		Iterator i;
		i = set.iterator();
        while(i.hasNext()){
        	Map.Entry element = (Map.Entry)i.next();
        	if(checkStockList(stockList, (String)element.getKey()) == 0){
        		stockList.add((String)element.getKey());
        	}
        }
	}

	private int checkStockList(ArrayList<String> stockList, String n) {
		int bit = 0;
		for (String name : stockList) {
			if (n.equals(name)) {
				bit = 1;
				break;
			}
		}
		return bit;
	}
	
	private void addNewOrders(Order order, HashMap<String, ArrayList<Order>> placeOrders) {
		ArrayList<Order> newOrders = placeOrders.get(order.getStockSymbol());
		if (newOrders == null) {
			newOrders = new ArrayList<Order>();
			newOrders.add(order);
			placeOrders.put(order.getStockSymbol(), newOrders);
		} else {
			newOrders.add(order);
			placeOrders.put(order.getStockSymbol(), newOrders);
		}
	}

}
