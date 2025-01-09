import Communication.IPriceListener;
import Communication.IQuoteListener;
import Communication.IQuoteReplyListener;
import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Orchestrator extends OrchestratorBase implements IQuoteListener, IPriceListener {
    protected final Double ExecutionTolerance = 0.000001;

    // add code
    private final HashMap<Integer, Quote> _quotes;
    private final HashMap<Integer, List<QuotePrice>> _quotePrices;
    private final IQuoteListener _pricer;
    private final List<IQuoteReplyListener> _listeners;

    public Orchestrator(IQuoteListener pricer) {
        super();
        // TODO Initialize data structure to hold quotes, and to hold quote prices
        _quotes = new HashMap<Integer, Quote>();
        _quotePrices = new HashMap<Integer, List<QuotePrice>>();
        _pricer = pricer;
        _listeners = new ArrayList<IQuoteReplyListener>();
    }

    public void addListener(IQuoteReplyListener listener) {
        _listeners.add(listener);
    }

    private void cleanQuote(int quoteId) {
        _quotes.remove(quoteId);
        _quotePrices.remove(quoteId);
    }

    private boolean validate(QuoteExecutionPrice exec, List<QuotePrice> prices) {
        for (var price : prices)
        {
            if (Math.abs(price.Price - exec.ExecutionPrice) < ExecutionTolerance) {
                return true;
            }
        };
        return false;
    }
    private List<QuotePrice> getLastPrices(List<QuotePrice> prices, int n) {
        if (prices.size() <= n) {
            return new ArrayList<>(prices);
        }
        return prices.subList(prices.size() - n, prices.size());
    }


    public void start() {
    }

    public void stop() {
        _quotes.clear();
        _quotePrices.clear();
    }

    @Override
    public synchronized void onNewQuoteRequest(Quote quote) {
        _quotes.put(quote.getQuoteId(), quote);
        _quotePrices.put(quote.getQuoteId(), new ArrayList<>());
        System.out.println("[*ORCHESTRATOR*] Receiving a new quote request " + quote);
        _pricer.onNewQuoteRequest(quote);
        _listeners.forEach(listener -> {
            listener.onQuoteReply(quote);
        });
    }

    @Override
    public synchronized void onStopQuoteRequest(Quote quote) {
        _quotes.remove(quote.getQuoteId());
        _quotePrices.remove(quote.getQuoteId());
        System.out.println("[*ORCHESTRATOR*] Stopped " + quote);
        _pricer.onStopQuoteRequest(quote);
        _listeners.forEach(listener -> {
            listener.onQuoteStoppedReply(quote);
        });

    }

    @Override
    public synchronized void onExecuteQuoteRequest(QuoteExecutionPrice quoteExecutionPrice) {
        var anyValidPrice = false;
        // TODO check if there is a quote price  from the last 3 quote prices
        //  that is equal to execution price within EXECUTION Tolerance
        var lastPrices = getLastPrices(_quotePrices.get(quoteExecutionPrice.Quote.getQuoteId()), 3);
        anyValidPrice =  validate(quoteExecutionPrice, lastPrices);

        if (anyValidPrice)
        {
            quoteExecutionPrice.Status = ExecutionStatus.Success;
        }
        else
        {
            quoteExecutionPrice.Status = ExecutionStatus.Fail;
        }
        System.out.println("[*ORCHESTRATOR*] Execution of " + quoteExecutionPrice);
        // TODO clean unwanted quotes and quote prices
        cleanQuote(quoteExecutionPrice.Quote.getQuoteId());
        _pricer.onExecuteQuoteRequest(quoteExecutionPrice);
        _listeners.forEach(listener -> {
            listener.onExecuteQuoteReply(quoteExecutionPrice);
        });

    }

    @Override
    public void onPriceUpdate(QuotePrice quotePrice) {
        System.out.println("[*ORCHESTRATOR*] Receiving a price for " + quotePrice.Quote + " -> " + quotePrice.Price);
        _quotePrices.get(quotePrice.Quote.getQuoteId()).add(quotePrice);
        _listeners.forEach(listener -> {
            listener.onPriceUpdateReply(quotePrice);
        });
    }
}
