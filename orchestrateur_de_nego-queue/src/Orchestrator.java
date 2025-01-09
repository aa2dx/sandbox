import Communication.IPricerQueues;
import Communication.IQuoteQueues;
import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class Orchestrator extends OrchestratorBase {
    protected final Double ExecutionTolerance = 0.000001;
    private ScheduledExecutorService _scheduler;

    // add code
    private final HashMap<Integer, Quote> _quotes;
    private final HashMap<Integer, List<QuotePrice>> _quotePrices;

    public Orchestrator(IQuoteQueues quoteQueues, IPricerQueues pricerQueues) {
        super(quoteQueues, pricerQueues);
        // TODO Initialize data structure to hold quotes, and to hold quote prices
        _quotes = new HashMap<Integer, Quote>();
        _quotePrices = new HashMap<Integer, List<QuotePrice>>();
    }

    @Override
    protected void OnQuoteRequest(Quote o) {
        // TODO Add quotes to the data structure
        NotifyQuoteReceived(o);
        _quotes.put(o.getQuoteId(), o);
        _quotePrices.put(o.getQuoteId(), new ArrayList<>());
    }

    @Override
    protected void OnQuotePriceUpdate(QuotePrice o) {
        // TODO Update quote prices in quote prices data structure
        NotifyPriceUpdated(o);
        _quotePrices.get(o.Quote.getQuoteId()).add(o);
    }

    @Override
    protected void OnExecutionRequest(QuoteExecutionPrice o) {
        
        var anyValidPrice = false;
        // TODO check if there is a quote price  from the last 3 quote prices
        //  that is equal to execution price within EXECUTION Tolerance
        var lastPrices = getLastPrices(_quotePrices.get(o.Quote.getQuoteId()), 3);
        anyValidPrice =  validate(o, lastPrices);

        if (anyValidPrice)
        {
            o.Status = ExecutionStatus.Success;
            NotifyQuoteExecuted(o);
        }
        else
        {
            o.Status = ExecutionStatus.Fail;
            NotifyQuoteExecuted(o);
        }

        // TODO clean unwanted quotes and quote prices
        cleanQuote(o.Quote.getQuoteId());
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

    @Override
    protected void OnQuoteStop(Quote o) {
        // TODO clean unwanted quotes and quote prices
        NotifyQuoteStopped(o);
        cleanQuote(o.getQuoteId());
    }

    @Override
    public void Start() {
        super.Start();
        
        _scheduler = Executors.newScheduledThreadPool(1);
        _scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // TODO Schedule a pricing task
                // We should ask for a pricing for each quote that we have in memory
                // Communication with pricer can be done using the method RequestNewPrice
                _quotes.forEach((id, quote) -> RequestNewPrice(quote));

            }
        }, 1, 2, TimeUnit.SECONDS);

    }

    @Override
    public void Stop() {
        super.Stop();
        _quotes.clear();
        _quotePrices.clear();
        _scheduler.shutdown();
    }
}
