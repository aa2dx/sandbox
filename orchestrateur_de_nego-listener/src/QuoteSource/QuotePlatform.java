package QuoteSource;

import Communication.*;
import utils.CurrencyPair;
import utils.Quote;
import utils.QuoteExecutionPrice;
import utils.QuotePrice;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class QuotePlatform implements IQuoteReplyListener {
    String ScenarioFilePath = "Resources/SmallScenario.txt";
    IQuoteQueues _quoteQueues;
    private final HashMap<Integer, Quote> ProcessingQuotes = new HashMap<Integer, Quote>();
    private final ConcurrentHashMap<Integer, Double> QuotePrices = new ConcurrentHashMap<Integer, Double>();

    private final IQuoteListener _quoteListener;

    public QuotePlatform(IQuoteListener quoteListner) {
        _quoteListener = quoteListner;
    }

    public void run() {

        try(BufferedReader br = new BufferedReader(new FileReader(ScenarioFilePath))) {
            var scenarioProcessSpeed = Integer.parseInt(br.readLine());
            String line = br.readLine();

            while (line != null) {
                Process(line);
                line = br.readLine();
                Thread.sleep(scenarioProcessSpeed);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void Process(String line) {
        //Examples:
        // 1000
        // CREATE 1 EUR USD
        // EXECUTE 1 0.001
        // STOP 1
        // WAIT
        // END
        var splittedLine = line.split(" ");

        switch (splittedLine[0]) {
            case "CREATE":
                var createQuoteId = Integer.parseInt(splittedLine[1]);
                var currency1 = splittedLine[2];
                var currency2 = splittedLine[3];
                var quote = new Quote(createQuoteId, new CurrencyPair(currency1, currency2));
                ProcessingQuotes.put(createQuoteId, quote);
                _quoteListener.onNewQuoteRequest(quote);
                break;
            case "EXECUTE":
                var executeQuoteId = Integer.parseInt(splittedLine[1]);
                var executionDelta = Double.parseDouble(splittedLine[2]);
                var lastPrice = QuotePrices.getOrDefault(executeQuoteId, 0.0);

                QuoteExecutionPrice quoteExecutionPrice = new QuoteExecutionPrice();
                quoteExecutionPrice.Quote = ProcessingQuotes.get(executeQuoteId);
                quoteExecutionPrice.ExecutionPrice = lastPrice + executionDelta;

                ProcessingQuotes.remove(executeQuoteId);
                _quoteListener.onExecuteQuoteRequest(quoteExecutionPrice);
                break;
            case "STOP":
                var stopQuoteId = Integer.parseInt(splittedLine[1]);
                var stopQuote = ProcessingQuotes.get(stopQuoteId);
                ProcessingQuotes.remove(stopQuoteId);
                _quoteListener.onStopQuoteRequest(stopQuote);
                break;
            case "WAIT":
                break;
            case "END":
                for (var endId:ProcessingQuotes.keySet()) {
                    var endQuote = ProcessingQuotes.get(endId);
                    _quoteListener.onStopQuoteRequest(endQuote);
                }
                ProcessingQuotes.clear();
                break;
            default:
                throw new IllegalArgumentException("Can't process scenario, unknown command");
        }
    }

    @Override
    public void onQuoteReply(Quote quote) {
        System.out.println("[~~~PLATFORM~~~] Started " + quote);
    }

    @Override
    public void onQuoteStoppedReply(Quote quote) {
        System.out.println("[~~~PLATFORM~~~] Stopped " + quote);
    }

    @Override
    public void onPriceUpdateReply(QuotePrice quotePrice) {
        QuotePrices.compute(quotePrice.Quote.getQuoteId(), (k, v) -> quotePrice.Price);
        System.out.println("[~~~PLATFORM~~~] Price update for " + quotePrice);
    }

    @Override
    public void onExecuteQuoteReply(QuoteExecutionPrice quoteExecution) {
            System.out.println("[~~~PLATFORM~~~] Execution of " + quoteExecution);
    }
}
