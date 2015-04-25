package pkg.market;

import pkg.exception.StockMarketExpection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pkg.market.api.IObserver;
import pkg.market.api.ISubject;
import pkg.stock.Stock;

public class MarketHistory implements IObserver {
	private ISubject stockMarketSubject;
	Market market;
	Map<String, List<Double>> history;

	public MarketHistory(Market m) {
		super();
		this.market = m;
		history = new HashMap<String, List<Double>>();
	}

	@Override
	public void setSubject(ISubject priceSetter) {
		this.stockMarketSubject = priceSetter;
	}

	public void startHistoryWithPrice(String symbol, Double newPrice)
			throws StockMarketExpection {
		if (!history.containsKey(symbol)) {
			List<Double> priceList = new ArrayList<Double>();
			priceList.add(newPrice);
			history.put(symbol, priceList);
		}
	}

	@Override
	public void update() {
		Stock updatedStock = (Stock) stockMarketSubject.getUpdate();
		if (market.getStockForSymbol(updatedStock.getSymbol()) == null) {
			return;
		}
		if (history.containsKey(updatedStock.getSymbol())) {
			List<Double> priceList = history.get(updatedStock.getSymbol());
			priceList.add(updatedStock.getPrice());
			history.put(updatedStock.getSymbol(), priceList);
		} else {
			
			List<Double> priceList = new ArrayList<Double>();
			priceList.add(updatedStock.getPrice());
			history.put(updatedStock.getSymbol(), priceList);
		}
	}

	public ArrayList<Double> getPriceFor(String symbol) {
		if (history.containsKey(symbol)) {
			return (ArrayList<Double>) history.get(symbol);
		} else {
			return new ArrayList<Double>();
		}
	}
}
