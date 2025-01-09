package Pricer;

import Communication.IPriceListener;
import Communication.IQuoteListener;
import utils.CurrencyPair;
import utils.Quote;
import utils.QuoteExecutionPrice;
import utils.QuotePrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpotPricer implements IQuoteListener, Runnable {

    private final Map<Integer, Quote> _quotes;
    private final ArrayList<IPriceListener> _listeners;
//    private final Thread _thread;
    private boolean _stop;
    private ScheduledExecutorService _scheduler;

    public SpotPricer() {
        _quotes = new HashMap<Integer, Quote>();
        _listeners = new ArrayList<IPriceListener>();
//        _thread = new Thread(this);
        _stop = true;
    }


    public void start() {
        _stop = false;
        // _thread.start();
        _scheduler = Executors.newScheduledThreadPool(1);
        _scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                priceAllQuotes();
            }
        }, 1, 2, TimeUnit.SECONDS);

    }

    @Override
    public void run() {
        while (!_stop) {
            priceAllQuotes();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private synchronized void priceAllQuotes() {
        if (_quotes.isEmpty()) return;
        _quotes.forEach((quoteId, quote) -> {
            var quotePrice = priceQuote(quote);
            System.out.println("[----PRICER----] Computing price for " + quotePrice.Quote.getQuoteId() + " -> " + quotePrice);
            _listeners.forEach(listener -> {
                listener.onPriceUpdate(quotePrice);
            });
        });
    }

    public void stop() {
        _stop = true;
        _scheduler.shutdown();
    }

    @Override
    public synchronized void onNewQuoteRequest(Quote quote) {
        _quotes.put(quote.getQuoteId(), quote);
    }

    @Override
    public synchronized void onStopQuoteRequest(Quote quote) {
        _quotes.remove(quote.getQuoteId());
    }

    @Override
    public synchronized void onExecuteQuoteRequest(QuoteExecutionPrice quoteExecution) {
        _quotes.remove(quoteExecution.Quote.getQuoteId());
    }

    private QuotePrice priceQuote(Quote quote) {
        var random = new Random();
        var variation = (((Double) random.nextDouble()) * 2.0 - 1.0) / 100.0;
        var newPrice = getCurrencyPairCoefficient(quote.getCurrencyPair()) + variation;
        BigDecimal bigDecimal = new BigDecimal(newPrice).setScale(5, RoundingMode.DOWN);
        var roundedNewPrice = bigDecimal.doubleValue();
        var quotePrice = new QuotePrice();
        quotePrice.Quote = quote;
        quotePrice.Price = roundedNewPrice;
        return quotePrice;
    }

    private Double getCurrencyPairCoefficient(CurrencyPair currencyPair) {
        return getCurrencyCoefficient(currencyPair.getCurrency1()) / getCurrencyCoefficient(currencyPair.getCurrency2());
    }

    private Double getCurrencyCoefficient(String currency) {
        switch (currency) {
            case "EUR":
                return 0.99;
            case "USD":
                return 1.0;
            case "CAD":
                return 0.74;
            case "GBP":
                return 1.13;
            case "JPY":
                return 0.0068;
        }
        return 1.0;
    }

    public void addListener(IPriceListener listener) {
        _listeners.add(listener);
    }
}
