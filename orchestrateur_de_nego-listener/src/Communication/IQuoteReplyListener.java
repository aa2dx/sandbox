package Communication;

import utils.Quote;
import utils.QuoteExecutionPrice;
import utils.QuotePrice;

public interface IQuoteReplyListener {
    void onQuoteReply(Quote quote);
    void onQuoteStoppedReply(Quote quote);
    void onPriceUpdateReply(QuotePrice quotePrice);
    void onExecuteQuoteReply(QuoteExecutionPrice quoteExecution);

}
