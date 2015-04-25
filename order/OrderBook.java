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
	Market m;
	HashMap<String, ArrayList<Order>> buyOrders;
	HashMap<String, ArrayList<Order>> sellOrders;

	public OrderBook(Market m) {
		this.m = m;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();
	}

	public void addToOrderBook(Order order) {

		if (order.getClass() == BuyOrder.class) {
			ArrayList<Order> newOrders = buyOrders.get(order.getStockSymbol());
			if (newOrders == null) {
				newOrders = new ArrayList<Order>();
				newOrders.add(order);
				buyOrders.put(order.getStockSymbol(), newOrders);
			} else {
				newOrders.add(order);
				buyOrders.put(order.getStockSymbol(), newOrders);
			}
		} else {
			ArrayList<Order> newOrders = sellOrders.get(order.getStockSymbol());
			if (newOrders == null) {
				newOrders = new ArrayList<Order>();
				newOrders.add(order);
				sellOrders.put(order.getStockSymbol(), newOrders);
			} else {
				newOrders.add(order);
				sellOrders.put(order.getStockSymbol(), newOrders);
			}
		}
	}

	public void trade() throws StockMarketExpection {
		
		PriceSetter pS = new PriceSetter();
		Double [] price = new Double[40];
		HashMap<Double, Integer> bOrder = new HashMap<Double, Integer>();
		HashMap<Double, Integer> sOrder = new HashMap<Double, Integer>();
		ArrayList<Order> allBuyOrders;
		ArrayList<Order> allSellOrders;
		
		Set set = buyOrders.entrySet();
        Iterator i = set.iterator();
        ArrayList<String> stockList = new ArrayList<String>();
        while(i.hasNext()){
        	Map.Entry element = (Map.Entry)i.next();
        	if(checkStockList(stockList, (String)element.getKey()) == 0){
        		stockList.add((String)element.getKey());
        	}
        }
        set = sellOrders.entrySet();
        i = set.iterator();
        while(i.hasNext()){
        	Map.Entry element = (Map.Entry)i.next();
        	if(checkStockList(stockList, (String)element.getKey()) == 0){
        		stockList.add((String)element.getKey());
        	}
        }
        
        for (String n: stockList){
        	allBuyOrders = buyOrders.get(n);
        	allSellOrders = sellOrders.get(n);
        	int j = 0;
        	
        	double dupPrice=0;
        	int flag = 0;
        	int marketBuyOrder = 0;
        	int marketSellOrder = 0;
        	for (Order o: allBuyOrders){
        		if(o.isMarketOrder == true){
        			marketBuyOrder = o.getSize();
        		}
        		else{
        		price[j] = o.getPrice();
        		j++;
        		}
        	}
        	for (Order o : allSellOrders){
        		if(o.isMarketOrder == true){
        			marketSellOrder = o.getSize();
        		}
        		else{
        		dupPrice = o.getPrice();
        		for(int x=0; x< j ; x++){
        			if(price[x] == dupPrice){
        				flag = 1;
        			}
        		}
        		if(flag == 0){
        			price[j]=dupPrice;
        			j++;
        			}
        		}
        	}
        	
        	double temp =0;
        	for (int y=0; y<j-1;y++){
        		for (int x = 0; x < j-1-y; x++){
        		if(price[x]>price[x+1]){
        			temp = price[x];
        			price[x]=price[x+1];
        			price[x+1]= temp;
        			
        		}
        		}
        	}
        	int value = marketBuyOrder;
        	for ( int x = j-1 ; x >=0 ; x--){
        		
        		flag = 0;
        		for(Order o: allBuyOrders){
        			if (o.getPrice() == price[x]){
        				value = value + o.getSize();
        				bOrder.put(price[x], value);
        				flag = 1;
        			}
        		}
        			if(flag == 0){
        				bOrder.put(price[x], 0);
        			}
        		
        	}
        	value = marketSellOrder;
        	for ( int x = 0 ; x <= j-1 ; x++){
        		
        		flag = 0;
        		for(Order o: allSellOrders){
        			if (o.getPrice() == price[x]){
        				value = value +o.getSize();
        				sOrder.put(price[x], value);
        				flag = 1;
        			}
        		}
        			if(flag == 0){
        				sOrder.put(price[x], 0);
        			}
        		
        		
        		
        	}
        	
        	 
        	 double matchingPrice = 0;
        	 int minVol = 0;
        	 int maxVol = 0;
        	 int bVol =0 ;
        	 int sVol =  0;
        	 for (int x= 0; x<= j-1; x++){
        		 
        		 
        		 bVol = bOrder.get(price[x]);
        		 sVol = sOrder.get(price[x]);
        	
        	minVol = Math.min(bVol, sVol);
        	if(maxVol <= minVol && minVol != 0){
        		maxVol = minVol;
        		matchingPrice = price[x];
        	}
        	
        	 }
        	 
        	 int shift = 0;
        	 int pool = 0;
        	 ArrayList<Order> copy = (ArrayList<Order>)allBuyOrders.clone();
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
	        	
	        	copy = (ArrayList<Order>)allSellOrders.clone();
	        	shift =0 ;
	        	pool =0 ;
	        	for(Order o: copy){
	        		if(o.getPrice() <= matchingPrice){
	        			if(pool == maxVol){
	        				break;
	        			}
	        			if(o.getSize() + pool - maxVol > 0){
	        				shift = o.getSize() + pool - maxVol;
	        			}
	        			o.setSize(o.getSize() - shift);
	        			pool = pool + o.getSize();
	        			o.getTrader().tradePerformed(o, matchingPrice);
	        			allSellOrders.remove(o);
	        		}
	        	}
	        	
	        	
	    		m.getMarketHistory().setSubject(pS);
	    		pS.registerObserver(m.getMarketHistory());
	    		pS.setNewPrice(m, n, matchingPrice);
        	
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

}
