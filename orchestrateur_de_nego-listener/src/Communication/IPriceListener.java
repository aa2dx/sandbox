package Communication;

import utils.QuotePrice;

public interface IPriceListener {
    void onPriceUpdate(QuotePrice quotePrice);
}
