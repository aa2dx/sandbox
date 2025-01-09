package Communication;

import utils.Quote;
import utils.QuoteExecutionPrice;

public interface IQuoteListener {
    void onNewQuoteRequest(Quote quote);
    void onStopQuoteRequest(Quote quote);
    void onExecuteQuoteRequest(QuoteExecutionPrice quoteExecution);
}
